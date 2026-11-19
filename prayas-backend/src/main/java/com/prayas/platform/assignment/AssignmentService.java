package com.prayas.platform.assignment;

import com.prayas.platform.audit.AuditLog;
import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.domain.DomainRole;
import com.prayas.platform.tour.CampusTourDetails;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.HeadcountRevision;
import com.prayas.platform.tour.HeadcountRevisionRepository;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.tour.ProgrammeRepository;
import com.prayas.platform.tour.ProgrammeStatus;
import com.prayas.platform.tour.TourDateChangedEvent;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Puts coordinators and volunteers on approved tours.
 *
 * "Availability" here means: not already on another tour at the same time.
 * The database enforces that (constraint no_overlapping_shifts), so even two
 * people assigning the same volunteer at the same instant cannot double-book
 * them; the checks in this class exist to give a readable message first.
 */
@Service
public class AssignmentService {

    /** Tour times are stored as plain campus-clock times, so shifts are anchored to this zone. */
    static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Kolkata");

    private final TourAssignmentRepository assignments;
    private final ProgrammeRepository programmes;
    private final AppUserRepository users;
    private final AuditLogRepository auditLogs;
    private final CampusTourDetailsRepository tourDetails;
    private final HeadcountRevisionRepository headcounts;

    public AssignmentService(TourAssignmentRepository assignments,
                             ProgrammeRepository programmes,
                             AppUserRepository users,
                             AuditLogRepository auditLogs,
                             CampusTourDetailsRepository tourDetails,
                             HeadcountRevisionRepository headcounts) {
        this.assignments = assignments;
        this.programmes = programmes;
        this.users = users;
        this.auditLogs = auditLogs;
        this.tourDetails = tourDetails;
        this.headcounts = headcounts;
    }

    /**
     * A tour's date was moved. The shifts were planned for the old date and
     * times, so everyone is taken off; the tour needs assigning again.
     * Runs inside the transaction that approved the move.
     */
    @EventListener
    public void onTourDateChanged(TourDateChangedEvent event) {
        int released = assignments.releaseAllForProgramme(event.programmeId());
        if (released > 0) {
            auditLogs.save(AuditLog.byUser(event.actorUserId(), "ASSIGNMENTS_RELEASED", "Programme",
                    event.programmeId(), Map.of("count", released, "reason", "visit date changed")));
        }
    }

    /** Everything the given person is down to do, soonest first, for tours that are still approved. */
    @Transactional(readOnly = true)
    public List<MyAssignmentView> myAssignments(Long userId) {
        List<MyAssignmentView> result = new ArrayList<>();
        for (TourAssignment shift : assignments.findByUserIdAndStatusOrderByStartsAtAscIdAsc(
                userId, AssignmentStatus.ACTIVE)) {
            Programme programme = programmes.findById(shift.getProgrammeId()).orElse(null);
            if (programme == null || programme.getStatus() != ProgrammeStatus.APPROVED) {
                continue;
            }
            CampusTourDetails details = tourDetails.findByProgrammeId(programme.getId()).orElse(null);
            int students = headcounts.findTopByProgrammeIdOrderByRevisionNoDesc(programme.getId())
                    .map(HeadcountRevision::getStudentCount).orElse(0);
            result.add(new MyAssignmentView(
                    shift.getId(), programme.getId(), shift.getRole(), shift.getStartsAt(), shift.getEndsAt(),
                    programme.getVisitDate(),
                    details != null ? details.getSchool().getName() : "(no details)",
                    details != null ? details.getSchool().getVillageOrTown() : null,
                    details != null ? details.getSchool().getDistrict() : null,
                    students,
                    details != null ? details.getContactName() : null,
                    details != null ? details.getContactPhone() : null));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<AssignmentView> list(Long programmeId) {
        programmeOrThrow(programmeId);
        return toViews(assignments.findByProgrammeIdAndStatusOrderByStartsAtAscIdAsc(
                programmeId, AssignmentStatus.ACTIVE));
    }

    /** People of the right kind who are free for the whole visit (or the given part of it). */
    @Transactional(readOnly = true)
    public List<AssigneeOption> available(Long programmeId, AssignmentRole role, LocalTime from, LocalTime to) {
        Programme programme = approvedProgramme(programmeId);
        Instant[] range = range(programme, from, to);
        Set<Long> busy = assignments.findBusyUserIds(range[0], range[1]);
        return eligibleUsers(role).stream()
                .filter(u -> !busy.contains(u.getId()))
                .map(u -> new AssigneeOption(u.getId(), u.getFullName()))
                .toList();
    }

    @Transactional
    public AssignmentView assign(Long programmeId, AssignRequest request, AppUser actor) {
        Programme programme = approvedProgramme(programmeId);
        Instant[] range = range(programme, request.from(), request.to());
        AppUser person = eligiblePerson(request.userId(), request.role());

        TourAssignment saved = saveShift(new TourAssignment(
                programmeId, person.getId(), request.role(), range[0], range[1], actor.getId()), person);

        auditLogs.save(AuditLog.byUser(actor.getId(), "ASSIGNMENT_CREATED", "Programme", programmeId,
                Map.of("userId", person.getId(), "role", request.role().name())));
        return toViews(List.of(saved)).get(0);
    }

    /**
     * The person on {@code assignmentId} (a volunteer or a coordinator) leaves
     * at {@code request.at()} and someone of the same kind covers the rest of
     * that shift. Can be repeated any number of times in one tour.
     */
    @Transactional
    public List<AssignmentView> handover(Long assignmentId, HandoverRequest request, AppUser actor) {
        TourAssignment current = activeAssignment(assignmentId);
        Programme programme = approvedProgramme(current.getProgrammeId());
        if (request.newUserId().equals(current.getUserId())) {
            throw new IllegalArgumentException("Pick someone else to take over.");
        }

        Instant at = instantOn(programme, request.at());
        if (!at.isAfter(current.getStartsAt()) || !at.isBefore(current.getEndsAt())) {
            throw new IllegalArgumentException(
                    "The hand-over time must fall inside the current shift.");
        }
        AppUser replacement = eligiblePerson(request.newUserId(), current.getRole());

        Instant originalEnd = current.getEndsAt();
        current.endAt(at);
        assignments.saveAndFlush(current);   // free the time slot first, so the new shift can start exactly there
        saveShift(new TourAssignment(current.getProgrammeId(), replacement.getId(),
                current.getRole(), at, originalEnd, actor.getId()), replacement);

        auditLogs.save(AuditLog.byUser(actor.getId(), "ASSIGNMENT_HANDOVER", "Programme", current.getProgrammeId(),
                Map.of("fromUserId", current.getUserId(), "toUserId", replacement.getId(),
                        "role", current.getRole().name())));
        return list(current.getProgrammeId());
    }

    @Transactional
    public void release(Long assignmentId, AppUser actor) {
        TourAssignment assignment = activeAssignment(assignmentId);
        assignment.release();
        auditLogs.save(AuditLog.byUser(actor.getId(), "ASSIGNMENT_RELEASED", "Programme", assignment.getProgrammeId(),
                Map.of("userId", assignment.getUserId(), "role", assignment.getRole().name())));
    }

    // --- helpers ---------------------------------------------------------

    private TourAssignment saveShift(TourAssignment shift, AppUser person) {
        String busyMessage = person.getFullName() + " is already assigned to a tour at that time.";
        if (assignments.existsOverlap(person.getId(), shift.getStartsAt(), shift.getEndsAt())) {
            throw new IllegalStateException(busyMessage);
        }
        try {
            return assignments.saveAndFlush(shift);
        } catch (DataIntegrityViolationException e) {
            // Two people assigned the same person at the same moment: the database constraint caught it.
            throw new IllegalStateException(busyMessage);
        }
    }

    private Programme programmeOrThrow(Long id) {
        return programmes.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tour request " + id + " not found"));
    }

    private Programme approvedProgramme(Long id) {
        Programme programme = programmeOrThrow(id);
        if (programme.getStatus() != ProgrammeStatus.APPROVED) {
            throw new IllegalStateException("People can only be assigned to an approved tour.");
        }
        if (programme.getArrivalTime() == null || programme.getDepartureTime() == null) {
            throw new IllegalStateException("This tour has no arrival and departure time, so shifts can't be planned.");
        }
        return programme;
    }

    private TourAssignment activeAssignment(Long id) {
        TourAssignment assignment = assignments.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Assignment " + id + " not found"));
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new IllegalStateException("That assignment has already been released.");
        }
        return assignment;
    }

    /** [start, end) for the whole visit, or for the part of it between from and to (both on the visit date). */
    private Instant[] range(Programme programme, LocalTime from, LocalTime to) {
        LocalTime start = from != null ? from : programme.getArrivalTime();
        LocalTime end = to != null ? to : programme.getDepartureTime();
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("The end time must be after the start time.");
        }
        if (start.isBefore(programme.getArrivalTime()) || end.isAfter(programme.getDepartureTime())) {
            throw new IllegalArgumentException("A shift has to fall within the visit (arrival to departure).");
        }
        return new Instant[]{instantOn(programme, start), instantOn(programme, end)};
    }

    private Instant instantOn(Programme programme, LocalTime time) {
        return programme.getVisitDate().atTime(time).atZone(CAMPUS_ZONE).toInstant();
    }

    private AppUser eligiblePerson(Long userId, AssignmentRole role) {
        return eligibleUsers(role).stream()
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        role == AssignmentRole.VOLUNTEER
                                ? "That person isn't an active volunteer (or a coordinator cleared to fill volunteer slots)."
                                : "That person isn't an active coordinator or head."));
    }

    private List<AppUser> eligibleUsers(AssignmentRole role) {
        return role == AssignmentRole.VOLUNTEER
                ? users.findEligible(List.of(DomainRole.VOLUNTEER), true)
                : users.findEligible(List.of(DomainRole.COORDINATOR, DomainRole.HEAD), false);
    }

    private List<AssignmentView> toViews(List<TourAssignment> rows) {
        Map<Long, AppUser> byId = users.findAllById(
                        rows.stream().map(TourAssignment::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return rows.stream()
                .map(a -> new AssignmentView(a.getId(), a.getUserId(),
                        byId.containsKey(a.getUserId()) ? byId.get(a.getUserId()).getFullName() : "Unknown",
                        a.getRole(), a.getStartsAt(), a.getEndsAt(), a.getStatus()))
                .toList();
    }
}
