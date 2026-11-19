package com.prayas.platform.approval;

import com.prayas.platform.audit.AuditLog;
import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.authority.AuthorityContact;
import com.prayas.platform.authority.AuthorityContactRepository;
import com.prayas.platform.requirement.RequirementKind;
import com.prayas.platform.requirement.RequirementRepository;
import com.prayas.platform.requirement.RequirementStatus;
import com.prayas.platform.tour.CampusTourDetails;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.HeadcountRevision;
import com.prayas.platform.tour.HeadcountRevisionRepository;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.tour.ProgrammeRepository;
import com.prayas.platform.tour.ProgrammeStatus;
import com.prayas.platform.tour.TourApprovedEvent;
import com.prayas.platform.tour.TourDateChangedEvent;
import com.prayas.platform.tour.TourInterestRepository;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import com.prayas.platform.venue.Venue;
import com.prayas.platform.venue.VenueRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tracks the permissions a tour needs from outside PRAYAS: one step for each
 * chosen venue that needs approval, and a two-step chain for lunch (hostel
 * office first, then the mess in-charge).
 *
 * Steps are created when a tour is approved. People then record progress on
 * them (contacted, approved, denied), and the status of the matching
 * requirement (Venue approvals, Lunch) is worked out from the steps, so the
 * team's task list and the detailed steps never disagree.
 */
@Service
public class ApprovalService {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Kolkata");

    private final ApprovalStepRepository steps;
    private final ProgrammeRepository programmes;
    private final CampusTourDetailsRepository tourDetails;
    private final HeadcountRevisionRepository headcounts;
    private final TourInterestRepository interests;
    private final VenueRepository venues;
    private final AuthorityContactRepository contacts;
    private final AppUserRepository users;
    private final RequirementRepository requirements;
    private final AuditLogRepository auditLogs;

    public ApprovalService(ApprovalStepRepository steps,
                           ProgrammeRepository programmes,
                           CampusTourDetailsRepository tourDetails,
                           HeadcountRevisionRepository headcounts,
                           TourInterestRepository interests,
                           VenueRepository venues,
                           AuthorityContactRepository contacts,
                           AppUserRepository users,
                           RequirementRepository requirements,
                           AuditLogRepository auditLogs) {
        this.steps = steps;
        this.programmes = programmes;
        this.tourDetails = tourDetails;
        this.headcounts = headcounts;
        this.interests = interests;
        this.venues = venues;
        this.contacts = contacts;
        this.users = users;
        this.requirements = requirements;
        this.auditLogs = auditLogs;
    }

    // --- reacting to what happens to a tour ----------------------------------

    /**
     * A tour was just approved: open the steps it needs. Runs inside the
     * approval's own transaction (a plain @EventListener), so the tour never
     * ends up approved without its steps. Safe to run twice.
     */
    @EventListener
    public void onTourApproved(TourApprovedEvent event) {
        Long programmeId = event.programmeId();
        Set<String> existing = steps.findByProgrammeIdOrderBySequenceAscIdAsc(programmeId).stream()
                .map(s -> s.getKind() + ":" + s.getVenueId())
                .collect(Collectors.toSet());

        for (Venue venue : interests.venuesNeedingApproval(programmeId)) {
            if (!existing.contains(ApprovalKind.VENUE + ":" + venue.getId())) {
                Long contactId = venue.getAuthorityContact() != null ? venue.getAuthorityContact().getId() : null;
                steps.save(new ApprovalStep(programmeId, ApprovalKind.VENUE, venue.getId(), contactId,
                        1, ApprovalStatus.PENDING));
            }
        }

        boolean lunch = tourDetails.findByProgrammeId(programmeId).map(CampusTourDetails::isLunchRequired).orElse(false);
        if (lunch) {
            if (!existing.contains(ApprovalKind.HOSTEL_OFFICE + ":null")) {
                steps.save(new ApprovalStep(programmeId, ApprovalKind.HOSTEL_OFFICE, null, null,
                        1, ApprovalStatus.PENDING));
            }
            if (!existing.contains(ApprovalKind.MESS + ":null")) {
                steps.save(new ApprovalStep(programmeId, ApprovalKind.MESS, null, null,
                        2, ApprovalStatus.WAITING));
            }
        }
    }

    /**
     * The tour moved to a new date. Permissions given for the old date don't
     * carry over, so every step that had moved on goes back to the start and
     * has to be asked again.
     */
    @EventListener
    public void onTourDateChanged(TourDateChangedEvent event) {
        List<ApprovalStep> all = steps.findByProgrammeIdOrderBySequenceAscIdAsc(event.programmeId());
        int reset = 0;
        for (ApprovalStep step : all) {
            if (step.getStatus() != ApprovalStatus.WAITING && step.getStatus() != ApprovalStatus.PENDING) {
                step.reset(ApprovalStatus.PENDING);
                reset++;
            }
        }
        enforceLunchChain(all);
        syncRequirements(event.programmeId(), all, false);
        if (reset > 0) {
            auditLogs.save(AuditLog.byUser(event.actorUserId(), "APPROVALS_RESET", "Programme",
                    event.programmeId(), Map.of("count", reset, "reason", "visit date changed")));
        }
    }

    // --- reading -------------------------------------------------------------

    /** Every step of one tour. */
    @Transactional(readOnly = true)
    public List<ApprovalStepView> forProgramme(Long programmeId) {
        if (!programmes.existsById(programmeId)) {
            throw new NoSuchElementException("Tour request " + programmeId + " not found");
        }
        return toViews(steps.findByProgrammeIdOrderBySequenceAscIdAsc(programmeId));
    }

    /**
     * The team's queue: upcoming approved tours that still have a permission
     * outstanding, soonest visit first, with all of each tour's steps.
     */
    @Transactional(readOnly = true)
    public List<ApprovalStepView> openQueue() {
        LocalDate today = LocalDate.now(CAMPUS_ZONE);
        Map<Long, List<ApprovalStep>> byTour = steps.findForUpcomingTours(ProgrammeStatus.APPROVED, today).stream()
                .collect(Collectors.groupingBy(ApprovalStep::getProgrammeId, LinkedHashMap::new, Collectors.toList()));
        List<ApprovalStep> open = new ArrayList<>();
        byTour.values().stream()
                .filter(group -> group.stream().anyMatch(s -> s.getStatus() != ApprovalStatus.APPROVED))
                .forEach(open::addAll);
        List<ApprovalStepView> views = toViews(open);
        // soonest visit first; steps of one tour stay together and in order
        List<ApprovalStepView> sorted = new ArrayList<>(views);
        sorted.sort(Comparator.comparing(ApprovalStepView::visitDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ApprovalStepView::programmeId));
        return sorted;
    }

    // --- acting ---------------------------------------------------------------

    @Transactional
    public ApprovalStepView update(Long stepId, ApprovalUpdateRequest request, AppUser actor) {
        ApprovalStep step = steps.findById(stepId)
                .orElseThrow(() -> new NoSuchElementException("Approval step " + stepId + " not found"));
        if (step.getStatus() == ApprovalStatus.WAITING) {
            throw new IllegalStateException(
                    "This step opens once the one before it has been approved.");
        }
        if (request.status() == ApprovalStatus.WAITING) {
            throw new IllegalArgumentException("A step can't be set back to waiting by hand.");
        }
        Programme programme = programmes.findById(step.getProgrammeId())
                .orElseThrow(() -> new NoSuchElementException("Tour request " + step.getProgrammeId() + " not found"));
        if (programme.getStatus() != ProgrammeStatus.APPROVED) {
            throw new IllegalStateException("Approvals can only be recorded for an approved tour.");
        }

        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        if (request.status() == ApprovalStatus.DENIED && note == null) {
            throw new IllegalArgumentException("Say why it was denied, so the next person knows what to fix.");
        }
        if (request.authorityContactId() != null) {
            AuthorityContact contact = contacts.findById(request.authorityContactId())
                    .filter(AuthorityContact::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("That contact isn't in the directory."));
            step.setAuthorityContactId(contact.getId());
        }

        ApprovalStatus before = step.getStatus();
        step.update(request.status(), note, actor.getId());

        List<ApprovalStep> all = steps.findByProgrammeIdOrderBySequenceAscIdAsc(step.getProgrammeId());
        enforceLunchChain(all);
        syncRequirements(step.getProgrammeId(), all, true);

        auditLogs.save(AuditLog.byUser(actor.getId(), "APPROVAL_" + request.status().name(), "Programme",
                step.getProgrammeId(), Map.of("stepId", step.getId(), "kind", step.getKind().name(),
                        "from", before.name(), "note", note == null ? "" : note)));
        return toViews(List.of(step)).get(0);
    }

    // --- rules ----------------------------------------------------------------

    /**
     * The mess step opens when the hostel step is approved and closes again
     * (losing whatever was recorded on it) if the hostel step is undone.
     */
    void enforceLunchChain(List<ApprovalStep> all) {
        ApprovalStep hostel = all.stream().filter(s -> s.getKind() == ApprovalKind.HOSTEL_OFFICE).findFirst().orElse(null);
        ApprovalStep mess = all.stream().filter(s -> s.getKind() == ApprovalKind.MESS).findFirst().orElse(null);
        if (hostel == null || mess == null) {
            return;
        }
        if (hostel.getStatus() == ApprovalStatus.APPROVED) {
            if (mess.getStatus() == ApprovalStatus.WAITING) {
                mess.reset(ApprovalStatus.PENDING);
            }
        } else if (mess.getStatus() != ApprovalStatus.WAITING) {
            mess.reset(ApprovalStatus.WAITING);
        }
    }

    /** How a group of steps reads as one task: all approved is done, any denial blocks it, any progress is in progress. */
    static RequirementStatus rollUp(List<ApprovalStep> group) {
        if (group.isEmpty()) {
            return RequirementStatus.PENDING;
        }
        if (group.stream().allMatch(s -> s.getStatus() == ApprovalStatus.APPROVED)) {
            return RequirementStatus.DONE;
        }
        if (group.stream().anyMatch(s -> s.getStatus() == ApprovalStatus.DENIED)) {
            return RequirementStatus.BLOCKED;
        }
        if (group.stream().anyMatch(s -> s.getStatus() == ApprovalStatus.REQUESTED
                || s.getStatus() == ApprovalStatus.APPROVED)) {
            return RequirementStatus.IN_PROGRESS;
        }
        return RequirementStatus.PENDING;
    }

    private void syncRequirements(Long programmeId, List<ApprovalStep> all, boolean acknowledged) {
        List<ApprovalStep> venueSteps = all.stream().filter(s -> s.getKind() == ApprovalKind.VENUE).toList();
        List<ApprovalStep> lunchSteps = all.stream()
                .filter(s -> s.getKind() == ApprovalKind.HOSTEL_OFFICE || s.getKind() == ApprovalKind.MESS).toList();
        if (!venueSteps.isEmpty()) {
            requirements.findByProgrammeIdAndKind(programmeId, RequirementKind.VENUE_APPROVALS)
                    .ifPresent(r -> r.syncStatus(rollUp(venueSteps), acknowledged));
        }
        if (!lunchSteps.isEmpty()) {
            requirements.findByProgrammeIdAndKind(programmeId, RequirementKind.LUNCH)
                    .ifPresent(r -> r.syncStatus(rollUp(lunchSteps), acknowledged));
        }
    }

    // --- views -----------------------------------------------------------------

    private List<ApprovalStepView> toViews(List<ApprovalStep> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Programme> tours = programmes.findAllById(
                        rows.stream().map(ApprovalStep::getProgrammeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Programme::getId, Function.identity()));
        Map<Long, Venue> venueById = venues.findAllById(
                        rows.stream().map(ApprovalStep::getVenueId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Venue::getId, Function.identity()));
        Map<Long, AuthorityContact> contactById = contacts.findAllById(
                        rows.stream().map(ApprovalStep::getAuthorityContactId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(AuthorityContact::getId, Function.identity()));
        Map<Long, AppUser> deciders = users.findAllById(
                        rows.stream().map(ApprovalStep::getDecidedBy).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));

        Map<Long, String> schoolByTour = new HashMap<>();
        Map<Long, Integer> mealsByTour = new HashMap<>();
        for (Long tourId : tours.keySet()) {
            schoolByTour.put(tourId, tourDetails.findByProgrammeId(tourId)
                    .map(d -> d.getSchool().getName()).orElse("(no details)"));
            headcounts.findTopByProgrammeIdOrderByRevisionNoDesc(tourId)
                    .map(HeadcountRevision::getVegMeals).ifPresent(m -> mealsByTour.put(tourId, m));
        }

        List<ApprovalStepView> views = new ArrayList<>();
        for (ApprovalStep s : rows) {
            Programme tour = tours.get(s.getProgrammeId());
            Venue venue = s.getVenueId() != null ? venueById.get(s.getVenueId()) : null;
            AuthorityContact contact = s.getAuthorityContactId() != null ? contactById.get(s.getAuthorityContactId()) : null;
            AppUser decider = s.getDecidedBy() != null ? deciders.get(s.getDecidedBy()) : null;
            Integer meals = mealsByTour.get(s.getProgrammeId());
            String detail = switch (s.getKind()) {
                case VENUE -> venue != null && venue.getDepartment() != null ? venue.getDepartment() : null;
                case HOSTEL_OFFICE, MESS -> meals != null ? meals + " meals" : null;
            };
            views.add(new ApprovalStepView(
                    s.getId(), s.getProgrammeId(),
                    tour != null ? tour.getVisitDate() : null,
                    schoolByTour.getOrDefault(s.getProgrammeId(), "(no details)"),
                    s.getKind(), title(s, venue), detail, s.getStatus(), s.getNote(),
                    decider != null ? decider.getFullName() : null, s.getDecidedAt(),
                    contact == null ? null : new ApprovalStepView.Contact(
                            contact.getId(), contact.getName(), contact.getOffice(), contact.getPhone(),
                            contact.getEmail(), contact.getOfficeHours(), contact.getPreferredContact())));
        }
        return views;
    }

    private static String title(ApprovalStep step, Venue venue) {
        return switch (step.getKind()) {
            case VENUE -> "Visit approval: " + (venue != null ? venue.getName() : "venue");
            case HOSTEL_OFFICE -> "Lunch, step 1: hostel office agrees to host the group";
            case MESS -> "Lunch, step 2: mess in-charge confirms the meals";
        };
    }
}
