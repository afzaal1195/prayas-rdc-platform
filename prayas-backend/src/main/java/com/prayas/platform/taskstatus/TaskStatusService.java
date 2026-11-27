package com.prayas.platform.taskstatus;

import com.prayas.platform.approval.ApprovalStatus;
import com.prayas.platform.approval.ApprovalStep;
import com.prayas.platform.approval.ApprovalStepRepository;
import com.prayas.platform.domain.Domain;
import com.prayas.platform.domain.DomainMembership;
import com.prayas.platform.domain.DomainMembershipRepository;
import com.prayas.platform.domain.DomainRole;
import com.prayas.platform.requirement.Requirement;
import com.prayas.platform.requirement.RequirementKind;
import com.prayas.platform.requirement.RequirementRepository;
import com.prayas.platform.requirement.RequirementStatus;
import com.prayas.platform.taskstatus.TaskStatusViews.*;
import com.prayas.platform.team.DomainAssignment;
import com.prayas.platform.team.DomainAssignmentRepository;
import com.prayas.platform.team.TeamRole;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.tour.ProgrammeRepository;
import com.prayas.platform.tour.ProgrammeStatus;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The task status board. The faculty in-charge and leads see every domain's
 * tasks; everyone else sees the domains they belong to (as head, coordinator
 * or volunteer). Only upcoming approved tours are shown.
 */
@Service
public class TaskStatusService {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Kolkata");

    private final ProgrammeRepository programmes;
    private final RequirementRepository requirements;
    private final ApprovalStepRepository steps;
    private final DomainMembershipRepository memberships;
    private final DomainAssignmentRepository assignments;
    private final AppUserRepository users;
    private final CampusTourDetailsRepository tourDetails;

    public TaskStatusService(ProgrammeRepository programmes,
                             RequirementRepository requirements,
                             ApprovalStepRepository steps,
                             DomainMembershipRepository memberships,
                             DomainAssignmentRepository assignments,
                             AppUserRepository users,
                             CampusTourDetailsRepository tourDetails) {
        this.programmes = programmes;
        this.requirements = requirements;
        this.steps = steps;
        this.memberships = memberships;
        this.assignments = assignments;
        this.users = users;
        this.tourDetails = tourDetails;
    }

    @Transactional(readOnly = true)
    public TaskBoard board(AppUser viewer) {
        boolean overview = viewer.isLeadOrFaculty();
        Map<Long, DomainRole> myRoles = new HashMap<>();
        for (DomainMembership m : memberships.findByUserId(viewer.getId())) {
            myRoles.put(m.getDomain().getId(), m.getDomainRole());
        }
        if (!overview && myRoles.isEmpty()) {
            return new TaskBoard(false, counts(List.of()), List.of());
        }

        LocalDate today = LocalDate.now(CAMPUS_ZONE);
        List<Programme> tours = programmes.findByStatusAndVisitDateGreaterThanEqualOrderByVisitDateAscIdAsc(
                ProgrammeStatus.APPROVED, today);
        if (tours.isEmpty()) {
            return new TaskBoard(overview, counts(List.of()), List.of());
        }
        List<Long> tourIds = tours.stream().map(Programme::getId).toList();
        Map<Long, Programme> tourById = tours.stream().collect(Collectors.toMap(Programme::getId, Function.identity()));

        Map<Long, List<DomainAssignment>> teamByTour = assignments.findByProgrammeIdIn(tourIds)
                .stream().collect(Collectors.groupingBy(DomainAssignment::getProgrammeId));
        Map<Long, AppUser> people = users.findAllById(
                        teamByTour.values().stream().flatMap(List::stream).map(DomainAssignment::getUserId)
                                .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        Map<Long, List<ApprovalStep>> stepsByTour = steps.findForUpcomingTours(ProgrammeStatus.APPROVED, today)
                .stream().collect(Collectors.groupingBy(ApprovalStep::getProgrammeId));
        Map<Long, String> schools = new HashMap<>();

        Map<Domain, List<TaskRow>> byDomain = new TreeMap<>(Comparator.comparing(Domain::getName));
        for (Requirement r : requirements.findByProgrammeIds(tourIds)) {
            Domain domain = r.getDomain();
            DomainRole myRole = myRoles.get(domain.getId());
            if (!overview && myRole == null) {
                continue;
            }
            if (r.getStatus() == RequirementStatus.CANCELLED) {
                continue;
            }
            Programme tour = tourById.get(r.getProgrammeId());
            List<DomainAssignment> team = teamByTour.getOrDefault(r.getProgrammeId(), List.of()).stream()
                    .filter(a -> a.getDomainId().equals(domain.getId())).toList();
            boolean fromSteps = r.getKind() != RequirementKind.VOLUNTEERS;
            boolean canSet = !fromSteps && (overview || myRole == DomainRole.HEAD || myRole == DomainRole.COORDINATOR);
            byDomain.computeIfAbsent(domain, d -> new ArrayList<>()).add(new TaskRow(
                    r.getId(), r.getProgrammeId(),
                    schools.computeIfAbsent(r.getProgrammeId(), this::schoolName),
                    tour.getVisitDate(), r.getKind(), title(r.getKind()), r.getStatus(), r.needsAttention(),
                    r.getDueDate(), progress(r.getKind(), stepsByTour.getOrDefault(r.getProgrammeId(), List.of())),
                    names(team, TeamRole.COORDINATOR, people), names(team, TeamRole.VOLUNTEER, people), canSet));
        }

        List<DomainStatus> domains = new ArrayList<>();
        List<TaskRow> all = new ArrayList<>();
        for (Map.Entry<Domain, List<TaskRow>> e : byDomain.entrySet()) {
            List<TaskRow> rows = e.getValue();
            rows.sort(Comparator.comparing(TaskRow::visitDate).thenComparing(TaskRow::title));
            domains.add(new DomainStatus(e.getKey().getId(), e.getKey().getCode(), e.getKey().getName(),
                    counts(rows), rows));
            all.addAll(rows);
        }
        return new TaskBoard(overview, counts(all), domains);
    }

    static Counts counts(List<TaskRow> rows) {
        int pending = 0, inProgress = 0, done = 0, blocked = 0, attention = 0;
        for (TaskRow r : rows) {
            switch (r.status()) {
                case PENDING -> pending++;
                case IN_PROGRESS -> inProgress++;
                case DONE -> done++;
                case BLOCKED -> blocked++;
                default -> { }
            }
            if (r.needsAttention()) {
                attention++;
            }
        }
        return new Counts(rows.size(), pending, inProgress, done, blocked, attention);
    }

    /** "1 of 2 approvals done" for a task worked out from approval steps; null otherwise. */
    static String progress(RequirementKind kind, List<ApprovalStep> tourSteps) {
        List<ApprovalStep> mine = tourSteps.stream().filter(s -> switch (kind) {
            case VENUE_APPROVALS -> s.getKind() == com.prayas.platform.approval.ApprovalKind.VENUE;
            case LUNCH -> s.getKind() != com.prayas.platform.approval.ApprovalKind.VENUE;
            case VOLUNTEERS -> false;
        }).toList();
        if (mine.isEmpty()) {
            return null;
        }
        long approved = mine.stream().filter(s -> s.getStatus() == ApprovalStatus.APPROVED).count();
        return approved + " of " + mine.size() + (mine.size() == 1 ? " approval done" : " approvals done");
    }

    private static String title(RequirementKind kind) {
        return switch (kind) {
            case LUNCH -> "Lunch";
            case VENUE_APPROVALS -> "Venue approvals";
            case VOLUNTEERS -> "Volunteers";
        };
    }

    private String schoolName(Long programmeId) {
        return tourDetails.findByProgrammeId(programmeId).map(d -> d.getSchool().getName()).orElse("(no details)");
    }

    private static List<String> names(List<DomainAssignment> team, TeamRole role, Map<Long, AppUser> people) {
        return team.stream().filter(a -> a.getRole() == role)
                .map(a -> people.containsKey(a.getUserId()) ? people.get(a.getUserId()).getFullName() : "Unknown")
                .toList();
    }
}
