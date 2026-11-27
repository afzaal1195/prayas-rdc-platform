package com.prayas.platform.itinerary;

import com.prayas.platform.audit.AuditLog;
import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.itinerary.ItineraryViews.PublicStop;
import com.prayas.platform.itinerary.ItineraryViews.TourItinerary;
import com.prayas.platform.tour.AdvisoryLockDao;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.tour.ProgrammeRepository;
import com.prayas.platform.tour.ProgrammeStatus;
import com.prayas.platform.tour.TourProperties;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.venue.Venue;
import com.prayas.platform.venue.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The itinerary rules: visit window, no double-booking a tour or a venue. Pure unit tests -- no database. */
class ItineraryServiceTest {

    private static final Long TOUR = 5L;
    private static final Long LIBRARY_ID = 20L;

    private ItineraryStopRepository stops;
    private ProgrammeRepository programmes;
    private VenueRepository venues;
    private AuditLogRepository audit;
    private AdvisoryLockDao lock;
    private ItineraryService service;

    private final AppUser actor = user(1L, "Planner");
    private final Venue library = venue(LIBRARY_ID, "Library", true);

    @BeforeEach
    void setUp() {
        stops = mock(ItineraryStopRepository.class);
        programmes = mock(ProgrammeRepository.class);
        venues = mock(VenueRepository.class);
        audit = mock(AuditLogRepository.class);
        lock = mock(AdvisoryLockDao.class);
        service = new ItineraryService(stops, programmes, venues, mock(CampusTourDetailsRepository.class),
                audit, lock, new TourProperties());

        when(programmes.findById(TOUR)).thenReturn(Optional.of(tour(10, "10:00", "13:00")));
        when(venues.findById(LIBRARY_ID)).thenReturn(Optional.of(library));
    }

    // --- adding a stop ----------------------------------------------------------------

    @Test
    void aStopInsideTheVisitIsSavedUnderTheVenuesName() {
        service.addStop(TOUR, new AddStopRequest(LIBRARY_ID, null, t("10:30"), t("11:00"), null), actor);

        org.mockito.ArgumentCaptor<ItineraryStop> saved = org.mockito.ArgumentCaptor.forClass(ItineraryStop.class);
        verify(stops).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Library");
        assertThat(saved.getValue().getVenueId()).isEqualTo(LIBRARY_ID);
        verify(lock).lockDate(any());
        verify(audit).save(any(AuditLog.class));
    }

    @Test
    void aStopWithNoVenueNeedsATitle() {
        assertThatThrownBy(() -> service.addStop(TOUR, new AddStopRequest(null, " ", t("10:30"), t("11:00"), null), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("venue or give the stop a title");

        service.addStop(TOUR, new AddStopRequest(null, "Welcome talk", t("10:00"), t("10:30"), null), actor);
        verify(stops).save(any(ItineraryStop.class));
    }

    @Test
    void aStopMustEndAfterItStarts() {
        assertThatThrownBy(() -> service.addStop(TOUR, new AddStopRequest(null, "x", t("11:00"), t("11:00"), null), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("end after it starts");
    }

    @Test
    void aStopOutsideTheVisitWindowIsRefused() {
        assertThatThrownBy(() -> service.addStop(TOUR, new AddStopRequest(null, "Early", t("09:00"), t("10:30"), null), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inside the visit");
        assertThatThrownBy(() -> service.addStop(TOUR, new AddStopRequest(null, "Late", t("12:30"), t("13:30"), null), actor))
                .isInstanceOf(IllegalArgumentException.class);
        verify(stops, never()).save(any(ItineraryStop.class));
    }

    @Test
    void whenTheSchoolGaveNoTimesTheCampusDefaultsAreTheWindow() {
        when(programmes.findById(TOUR)).thenReturn(Optional.of(tour(10, null, null)));

        service.addStop(TOUR, new AddStopRequest(null, "Walk", t("09:30"), t("18:00"), null), actor);

        verify(stops).save(any(ItineraryStop.class));
    }

    @Test
    void aTourCannotBeInTwoPlacesAtOnce() {
        ItineraryStop existing = new ItineraryStop(TOUR, null, "Welcome talk", t("10:00"), t("11:00"), null, 1L);
        when(stops.findByProgrammeIdOrderByStartTimeAscIdAsc(TOUR)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.addStop(TOUR, new AddStopRequest(null, "Lab", t("10:30"), t("11:30"), null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlaps \"Welcome talk\"");
    }

    @Test
    void stopsThatMeetEndToEndAreNotAnOverlap() {
        ItineraryStop existing = new ItineraryStop(TOUR, null, "Welcome talk", t("10:00"), t("11:00"), null, 1L);
        when(stops.findByProgrammeIdOrderByStartTimeAscIdAsc(TOUR)).thenReturn(List.of(existing));

        service.addStop(TOUR, new AddStopRequest(null, "Lab", t("11:00"), t("11:30"), null), actor);

        verify(stops).save(any(ItineraryStop.class));
    }

    @Test
    void aVenueAnotherTourIsUsingAtTheSameTimeIsRefused() {
        ItineraryStop other = new ItineraryStop(9L, LIBRARY_ID, "Library", t("10:00"), t("11:00"), null, 2L);
        when(stops.findVenueClashes(anyLong(), any(), any(), any(), anyLong())).thenReturn(List.of(other));

        assertThatThrownBy(() -> service.addStop(TOUR, new AddStopRequest(LIBRARY_ID, null, t("10:30"), t("11:30"), null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Library is already in use");
        verify(stops, never()).save(any(ItineraryStop.class));
    }

    @Test
    void anInactiveVenueCannotBePicked() {
        Venue closed = venue(21L, "Old lab", false);
        when(venues.findById(21L)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.addStop(TOUR, new AddStopRequest(21L, null, t("10:30"), t("11:00"), null), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("isn't available");
    }

    @Test
    void onlyAnUpcomingApprovedTourCanBePlanned() {
        Programme submitted = new Programme(LocalDate.now().plusDays(10));
        when(programmes.findById(TOUR)).thenReturn(Optional.of(submitted));
        assertThatThrownBy(() -> service.addStop(TOUR, new AddStopRequest(null, "x", t("10:30"), t("11:00"), null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved tour");

        when(programmes.findById(TOUR)).thenReturn(Optional.of(tour(-2, "10:00", "13:00")));
        assertThatThrownBy(() -> service.addStop(TOUR, new AddStopRequest(null, "x", t("10:30"), t("11:00"), null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already taken place");
    }

    // --- removing ---------------------------------------------------------------------------

    @Test
    void aStopCanBeRemovedWhileTheTourIsStillAhead() {
        ItineraryStop stop = new ItineraryStop(TOUR, null, "Lunch", t("12:00"), t("12:30"), null, 1L);
        when(stops.findById(7L)).thenReturn(Optional.of(stop));

        service.removeStop(7L, actor);

        verify(stops).delete(stop);
    }

    @Test
    void aStopOfATourThatAlreadyHappenedCannotBeRemoved() {
        ItineraryStop stop = new ItineraryStop(TOUR, null, "Lunch", t("12:00"), t("12:30"), null, 1L);
        when(stops.findById(7L)).thenReturn(Optional.of(stop));
        when(programmes.findById(TOUR)).thenReturn(Optional.of(tour(-2, "10:00", "13:00")));

        assertThatThrownBy(() -> service.removeStop(7L, actor)).isInstanceOf(IllegalStateException.class);
        verify(stops, never()).delete(any(ItineraryStop.class));
    }

    // --- reading ------------------------------------------------------------------------------

    @Test
    void aStopThatNoLongerFitsAfterTheTourMovedIsFlagged() {
        Programme moved = tour(10, "11:00", "13:00");
        when(programmes.findByStatusAndVisitDateGreaterThanEqualOrderByVisitDateAscIdAsc(any(), any()))
                .thenReturn(List.of(moved));
        ItineraryStop early = new ItineraryStop(TOUR, null, "Welcome talk", t("10:00"), t("10:30"), null, 1L);
        ReflectionTestUtils.setField(early, "id", 1L);
        when(stops.findByProgrammeIdInOrderByStartTimeAscIdAsc(any())).thenReturn(List.of(early));

        List<TourItinerary> result = service.itineraries();

        assertThat(result.get(0).stops().get(0).problems()).anyMatch(p -> p.contains("Outside the visit"));
    }

    @Test
    void aVenueClashThatAppearedLaterIsFlagged() {
        when(programmes.findByStatusAndVisitDateGreaterThanEqualOrderByVisitDateAscIdAsc(any(), any()))
                .thenReturn(List.of(tour(10, "10:00", "13:00")));
        ItineraryStop mine = new ItineraryStop(TOUR, LIBRARY_ID, "Library", t("10:00"), t("11:00"), null, 1L);
        ReflectionTestUtils.setField(mine, "id", 1L);
        ItineraryStop other = new ItineraryStop(9L, LIBRARY_ID, "Library", t("10:30"), t("11:30"), null, 2L);
        when(stops.findByProgrammeIdInOrderByStartTimeAscIdAsc(any())).thenReturn(List.of(mine));
        when(stops.findVenueClashes(anyLong(), any(), any(), any(), anyLong())).thenReturn(List.of(other));
        when(venues.findAllById(any())).thenReturn(List.of(library));

        List<TourItinerary> result = service.itineraries();

        assertThat(result.get(0).stops().get(0).problems()).anyMatch(p -> p.contains("also booked"));
        assertThat(result.get(0).stops().get(0).venueName()).isEqualTo("Library");
    }

    @Test
    void aSchoolSeesItsStopsOnlyOnceTheTourIsApproved() {
        ItineraryStop stop = new ItineraryStop(TOUR, null, "Lunch", t("12:00"), t("12:30"), "kitchen is far", 1L);
        when(stops.findByProgrammeIdOrderByStartTimeAscIdAsc(TOUR)).thenReturn(List.of(stop));

        List<PublicStop> shown = service.forSchool(TOUR);
        assertThat(shown).extracting(PublicStop::title).containsExactly("Lunch");

        when(programmes.findById(TOUR)).thenReturn(Optional.of(new Programme(LocalDate.now().plusDays(10))));
        assertThat(service.forSchool(TOUR)).isEmpty();
    }

    // --- helpers ----------------------------------------------------------------------------------

    private static LocalTime t(String hhmm) {
        return LocalTime.parse(hhmm);
    }

    private static Programme tour(int daysFromToday, String arrival, String departure) {
        Programme programme = new Programme(LocalDate.now().plusDays(daysFromToday));
        programme.transitionTo(ProgrammeStatus.UNDER_REVIEW);
        programme.transitionTo(ProgrammeStatus.APPROVED);
        if (arrival != null) {
            programme.setArrivalTime(LocalTime.parse(arrival));
        }
        if (departure != null) {
            programme.setDepartureTime(LocalTime.parse(departure));
        }
        ReflectionTestUtils.setField(programme, "id", TOUR);
        return programme;
    }

    private static Venue venue(Long id, String name, boolean active) {
        Venue venue = new Venue(name, com.prayas.platform.venue.VenueType.LIBRARY);
        venue.setActive(active);
        ReflectionTestUtils.setField(venue, "id", id);
        return venue;
    }

    private static AppUser user(Long id, String name) {
        AppUser user = new AppUser(name.toLowerCase() + "@iith.ac.in", name);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
