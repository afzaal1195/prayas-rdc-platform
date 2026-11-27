package com.prayas.platform.itinerary;

import com.prayas.platform.audit.AuditLog;
import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.itinerary.ItineraryViews.*;
import com.prayas.platform.tour.AdvisoryLockDao;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.tour.ProgrammeRepository;
import com.prayas.platform.tour.ProgrammeStatus;
import com.prayas.platform.tour.TourProperties;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.venue.Venue;
import com.prayas.platform.venue.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Plans where an approved tour goes and when.
 *
 * A stop has to sit inside the tour's visit window (its arrival and departure, or the
 * campus's earliest and latest times when the school didn't say), and a tour can't be in two
 * places at once. A venue can only host one tour at a time: another approved tour using the
 * same venue on the same date at an overlapping time is refused. Those rules are checked when
 * a stop is added, and again whenever the itinerary is read, because the tour's date or times
 * can change afterwards and quietly break a stop that was fine when it was added.
 */
@Service
public class ItineraryService {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Kolkata");

    private final ItineraryStopRepository stops;
    private final ProgrammeRepository programmes;
    private final VenueRepository venues;
    private final CampusTourDetailsRepository tourDetails;
    private final AuditLogRepository auditLogs;
    private final AdvisoryLockDao advisoryLock;
    private final TourProperties props;

    public ItineraryService(ItineraryStopRepository stops,
                            ProgrammeRepository programmes,
                            VenueRepository venues,
                            CampusTourDetailsRepository tourDetails,
                            AuditLogRepository auditLogs,
                            AdvisoryLockDao advisoryLock,
                            TourProperties props) {
        this.stops = stops;
        this.programmes = programmes;
        this.venues = venues;
        this.tourDetails = tourDetails;
        this.auditLogs = auditLogs;
        this.advisoryLock = advisoryLock;
        this.props = props;
    }

    // --- reading -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Board board() {
        List<VenueOption> options = venues.findAllByOrderByNameAsc().stream()
                .filter(Venue::isActive)
                .map(v -> new VenueOption(v.getId(), v.getName()))
                .toList();
        return new Board(options, itineraries());
    }

    /** Every upcoming approved tour with its stops, each stop carrying any problem it has right now. */
    @Transactional(readOnly = true)
    public List<TourItinerary> itineraries() {
        LocalDate today = LocalDate.now(CAMPUS_ZONE);
        List<Programme> tours = programmes.findByStatusAndVisitDateGreaterThanEqualOrderByVisitDateAscIdAsc(
                ProgrammeStatus.APPROVED, today);
        if (tours.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ItineraryStop>> byTour = stops
                .findByProgrammeIdInOrderByStartTimeAscIdAsc(tours.stream().map(Programme::getId).toList())
                .stream().collect(Collectors.groupingBy(ItineraryStop::getProgrammeId));
        Map<Long, String> venueNames = venueNames(byTour.values().stream().flatMap(List::stream).toList());

        List<TourItinerary> result = new ArrayList<>();
        for (Programme tour : tours) {
            List<ItineraryStop> mine = byTour.getOrDefault(tour.getId(), List.of());
            LocalTime[] window = window(tour);
            List<Stop> views = new ArrayList<>();
            for (ItineraryStop stop : mine) {
                views.add(new Stop(stop.getId(), stop.getVenueId(), venueNames.get(stop.getVenueId()),
                        stop.getTitle(), stop.getStartTime(), stop.getEndTime(), stop.getNotes(),
                        problems(stop, mine, tour, window)));
            }
            result.add(new TourItinerary(tour.getId(), schoolName(tour.getId()), tour.getVisitDate(),
                    window[0], window[1], views));
        }
        return result;
    }

    /** The stops a school may see on its tracking page: only once the tour is approved. */
    @Transactional(readOnly = true)
    public List<PublicStop> forSchool(Long programmeId) {
        Programme tour = programmes.findById(programmeId).orElse(null);
        if (tour == null || tour.getStatus() != ProgrammeStatus.APPROVED) {
            return List.of();
        }
        List<ItineraryStop> mine = stops.findByProgrammeIdOrderByStartTimeAscIdAsc(programmeId);
        Map<Long, String> venueNames = venueNames(mine);
        return mine.stream()
                .map(s -> new PublicStop(s.getTitle(), venueNames.get(s.getVenueId()), s.getStartTime(), s.getEndTime()))
                .toList();
    }

    // --- changing ------------------------------------------------------------------------

    @Transactional
    public void addStop(Long programmeId, AddStopRequest request, AppUser actor) {
        Programme tour = programmes.findById(programmeId)
                .orElseThrow(() -> new NoSuchElementException("Tour request " + programmeId + " not found"));
        if (tour.getStatus() != ProgrammeStatus.APPROVED) {
            throw new IllegalStateException("An itinerary can only be planned for an approved tour.");
        }
        if (tour.getVisitDate().isBefore(LocalDate.now(CAMPUS_ZONE))) {
            throw new IllegalStateException("This tour has already taken place, so its itinerary can't be changed.");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("A stop has to end after it starts.");
        }
        LocalTime[] window = window(tour);
        if (request.startTime().isBefore(window[0]) || request.endTime().isAfter(window[1])) {
            throw new IllegalArgumentException("A stop has to fall inside the visit, which runs from %s to %s."
                    .formatted(window[0], window[1]));
        }

        Venue venue = null;
        if (request.venueId() != null) {
            venue = venues.findById(request.venueId())
                    .filter(Venue::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("That venue isn't available."));
        }
        String title = request.title() == null ? "" : request.title().trim();
        if (title.isEmpty()) {
            if (venue == null) {
                throw new IllegalArgumentException("Pick a venue or give the stop a title.");
            }
            title = venue.getName();
        }

        // Serialise with anyone else planning stops for this date, so two tours can't both
        // grab the same venue slot at the same moment.
        advisoryLock.lockDate(tour.getVisitDate());

        for (ItineraryStop other : stops.findByProgrammeIdOrderByStartTimeAscIdAsc(programmeId)) {
            if (overlaps(other.getStartTime(), other.getEndTime(), request.startTime(), request.endTime())) {
                throw new IllegalStateException("That overlaps \"%s\" (%s to %s) on this tour."
                        .formatted(other.getTitle(), other.getStartTime(), other.getEndTime()));
            }
        }
        if (venue != null) {
            List<ItineraryStop> clashes = stops.findVenueClashes(venue.getId(), tour.getVisitDate(),
                    request.startTime(), request.endTime(), programmeId);
            if (!clashes.isEmpty()) {
                ItineraryStop clash = clashes.get(0);
                throw new IllegalStateException("%s is already in use by %s from %s to %s."
                        .formatted(venue.getName(), schoolName(clash.getProgrammeId()),
                                clash.getStartTime(), clash.getEndTime()));
            }
        }

        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().trim();
        stops.save(new ItineraryStop(programmeId, venue != null ? venue.getId() : null, title,
                request.startTime(), request.endTime(), notes, actor.getId()));
        auditLogs.save(AuditLog.byUser(actor.getId(), "ITINERARY_STOP_ADDED", "Programme", programmeId,
                Map.of("title", title, "start", request.startTime().toString(), "end", request.endTime().toString())));
    }

    @Transactional
    public void removeStop(Long stopId, AppUser actor) {
        ItineraryStop stop = stops.findById(stopId)
                .orElseThrow(() -> new NoSuchElementException("Stop " + stopId + " not found"));
        Programme tour = programmes.findById(stop.getProgrammeId())
                .orElseThrow(() -> new NoSuchElementException("Tour request " + stop.getProgrammeId() + " not found"));
        if (tour.getVisitDate().isBefore(LocalDate.now(CAMPUS_ZONE))) {
            throw new IllegalStateException("This tour has already taken place, so its itinerary can't be changed.");
        }
        stops.delete(stop);
        auditLogs.save(AuditLog.byUser(actor.getId(), "ITINERARY_STOP_REMOVED", "Programme", stop.getProgrammeId(),
                Map.of("title", stop.getTitle())));
    }

    // --- helpers ----------------------------------------------------------------------------

    /** The visit window: the tour's own arrival and departure, else the campus's earliest and latest times. */
    LocalTime[] window(Programme tour) {
        LocalTime start = tour.getArrivalTime() != null ? tour.getArrivalTime() : LocalTime.parse(props.getEarliestArrival());
        LocalTime end = tour.getDepartureTime() != null ? tour.getDepartureTime() : LocalTime.parse(props.getLatestDeparture());
        return new LocalTime[] {start, end};
    }

    static boolean overlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && endA.isAfter(startB);
    }

    /** What is wrong with a stop now, found by looking again at the tour's current times and at other tours. */
    private List<String> problems(ItineraryStop stop, List<ItineraryStop> sameTour, Programme tour, LocalTime[] window) {
        List<String> problems = new ArrayList<>();
        if (stop.getStartTime().isBefore(window[0]) || stop.getEndTime().isAfter(window[1])) {
            problems.add("Outside the visit, which now runs from %s to %s.".formatted(window[0], window[1]));
        }
        for (ItineraryStop other : sameTour) {
            if (!other.getId().equals(stop.getId()) && other.getId() < stop.getId()
                    && overlaps(other.getStartTime(), other.getEndTime(), stop.getStartTime(), stop.getEndTime())) {
                problems.add("Overlaps \"%s\" on this tour.".formatted(other.getTitle()));
            }
        }
        if (stop.getVenueId() != null) {
            for (ItineraryStop clash : stops.findVenueClashes(stop.getVenueId(), tour.getVisitDate(),
                    stop.getStartTime(), stop.getEndTime(), tour.getId())) {
                problems.add("The venue is also booked by %s from %s to %s."
                        .formatted(schoolName(clash.getProgrammeId()), clash.getStartTime(), clash.getEndTime()));
            }
        }
        return problems;
    }

    private Map<Long, String> venueNames(Collection<ItineraryStop> forStops) {
        Set<Long> ids = forStops.stream().map(ItineraryStop::getVenueId).filter(Objects::nonNull).collect(Collectors.toSet());
        // A HashMap, not Map.of(): a stop with no venue looks up the key null, which an immutable map rejects.
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Venue venue : venues.findAllById(ids)) {
                names.put(venue.getId(), venue.getName());
            }
        }
        return names;
    }

    private String schoolName(Long programmeId) {
        return tourDetails.findByProgrammeId(programmeId).map(d -> d.getSchool().getName()).orElse("another tour");
    }
}
