package com.prayas.platform.team;

import com.prayas.platform.audit.AuditLog;
import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.domain.Domain;
import com.prayas.platform.domain.DomainMembershipRepository;
import com.prayas.platform.domain.DomainRepository;
import com.prayas.platform.domain.DomainRole;
import com.prayas.platform.requirement.Requirement;
import com.prayas.platform.requirement.RequirementKind;
import com.prayas.platform.requirement.RequirementRepository;
import com.prayas.platform.team.TeamViews.*;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.tour.ProgrammeRepository;
import com.prayas.platform.tour.ProgrammeStatus;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The chain of command for each approved tour.
 *
 * Faculty in-charge and leads can put anyone on any domain's team. A domain
 * head puts coordinators (from that domain) on it. A coordinator who has been
 * put on a tour's domain team puts volunteers (from that domain) on the same
 * team. A domain's team exists for a tour only if the domain has a job on it
 * (lunch and venue approvals for Hospitality & Logistics, volunteers for
 * Volunteer Coordination).
 *
 * Assigning someone here records who is responsible; it does not lock the
 * domain's task list to them, and it is separate from who covers which hours
 * of the visit day (see AssignmentService).
 */
@Service
public class DomainTeamService {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Kolkata");

    private final DomainAssignmentRepository assignments;
    private final ProgrammeRepository programmes;
    private final RequirementRepository requirements;
    private final DomainMembershipRepository memberships;
    private final DomainRepository domains;
    private final AppUserRepository users;
    private final CampusTourDetailsRepository tourDetails;
    private final AuditLogRepository auditLogs;

    public DomainTeamService(DomainAssignmentRepository assignments,
                             ProgrammeRepository programmes,
                             RequirementRepository requirements,
                             DomainMembershipRepository memberships,
                             DomainRepository domains,
                             AppUserRepository users,
                             CampusTourDetailsRepository tourDetails,
                             AuditLogRepository auditLogs) {
        this.assignments = assignments;
        this.programmes = programmes;
        this.requirements = requirements;
        this.memberships = memberships;
        this.domains = domains;
        this.users = users;
        this.tourDetails = tourDetails;
        this.auditLogs = auditLogs;
    }

    // --- who may do what ---------------------------------------------------------

    /** The chain of command, in one place. */
    boolean canAssign(AppUser actor, Domain domain, TeamRole role, Long programmeId) {
        if (actor.isLeadOrFaculty()) {
            return true;
        }
        if (memberships.existsByUserAndDomainCodeAndRole(actor.getId(), domain.getCode(), DomainRole.HEAD)) {
            return true;
        }
        // a coordinator already on this tour's team for the domain may add volunteers, not coordinators
        return role == TeamRole.VOLUNTEER
                && assignments.existsByProgrammeIdAndDomainIdAndUserIdAndRole(
                        programmeId, domain.getId(), actor.getId(), TeamRole.COORDINATOR);
    }

    /** People the actor may pick for the role: anyone active for a lead or faculty; otherwise the domain's own coordinators or volunteers. */
    private List<AppUser> candidates(AppUser actor, Domain domain, TeamRole role) {
        if (actor.isLeadOrFaculty()) {
            return users.findAll().stream()
                    .filter(AppUser::isActive)
                    .sorted(Comparator.comparing(AppUser::getFullName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        DomainRole domainRole = role == TeamRole.COORDINATOR ? DomainRole.COORDINATOR : DomainRole.VOLUNTEER;
        return memberships.findUsersByDomainAndRole(domain.getId(), domainRole);
    }

    // --- reading ---------------------------------------------------------------------

    /** Every upcoming approved tour with each domain team on it, as the actor may see and change them. */
    @Transactional(readOnly = true)
    public List<TourTeams> teams(AppUser actor) {
        LocalDate today = LocalDate.now(CAMPUS_ZONE);
        List<Programme> tours = programmes.findByStatusAndVisitDateGreaterThanEqualOrderByVisitDateAscIdAsc(
                ProgrammeStatus.APPROVED, today);
        if (tours.isEmpty()) {
            return List.of();
        }
        Map<Long, List<DomainAssignment>> byTour = assignments.findByProgrammeIdIn(
                        tours.stream().map(Programme::getId).toList())
                .stream().collect(Collectors.groupingBy(DomainAssignment::getProgrammeId));
        Map<Long, AppUser> people = users.findAllById(
                        byTour.values().stream().flatMap(List::stream).map(DomainAssignment::getUserId)
                                .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        Map<String, List<AppUser>> candidateCache = new HashMap<>();

        List<TourTeams> result = new ArrayList<>();
        for (Programme tour : tours) {
            List<DomainAssignment> onTour = byTour.getOrDefault(tour.getId(), List.of());
            List<DomainTeam> teams = new ArrayList<>();
            for (Map.Entry<Domain, List<String>> entry : jobsByDomain(tour.getId()).entrySet()) {
                Domain domain = entry.getKey();
                List<DomainAssignment> here = onTour.stream().filter(a -> a.getDomainId().equals(domain.getId())).toList();
                Set<Long> alreadyOn = here.stream().map(DomainAssignment::getUserId).collect(Collectors.toSet());
                boolean canCoordinator = canAssign(actor, domain, TeamRole.COORDINATOR, tour.getId());
                boolean canVolunteer = canAssign(actor, domain, TeamRole.VOLUNTEER, tour.getId());
                teams.add(new DomainTeam(
                        domain.getId(), domain.getCode(), domain.getName(), entry.getValue(),
                        members(here, TeamRole.COORDINATOR, people),
                        members(here, TeamRole.VOLUNTEER, people),
                        canCoordinator, canVolunteer,
                        canCoordinator ? options(actor, domain, TeamRole.COORDINATOR, alreadyOn, candidateCache) : List.of(),
                        canVolunteer ? options(actor, domain, TeamRole.VOLUNTEER, alreadyOn, candidateCache) : List.of()));
            }
            result.add(new TourTeams(tour.getId(), tour.getVisitDate(), schoolName(tour.getId()), teams));
        }
        return result;
    }

    /** The signed-in person's own domain jobs on upcoming approved tours. */
    @Transactional(readOnly = true)
    public List<DomainJob> myJobs(Long userId) {
        LocalDate today = LocalDate.now(CAMPUS_ZONE);
        List<DomainAssignment> mine = assignments.findByUserId(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        Map<Long, Programme> tours = programmes.findAllById(
                        mine.stream().map(DomainAssignment::getProgrammeId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Programme::getId, Function.identity()));
        Map<Long, Domain> domainById = domains.findAllById(
                        mine.stream().map(DomainAssignment::getDomainId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Domain::getId, Function.identity()));
        Map<Long, List<DomainAssignment>> byTour = assignments.findByProgrammeIdIn(tours.keySet())
                .stream().collect(Collectors.groupingBy(DomainAssignment::getProgrammeId));
        Map<Long, AppUser> people = users.findAllById(
                        byTour.values().stream().flatMap(List::stream).map(DomainAssignment::getUserId)
                                .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));

        List<DomainJob> jobs = new ArrayList<>();
        for (DomainAssignment a : mine) {
            Programme tour = tours.get(a.getProgrammeId());
            Domain domain = domainById.get(a.getDomainId());
            if (tour == null || domain == null || tour.getStatus() != ProgrammeStatus.APPROVED
                    || tour.getVisitDate().isBefore(today)) {
                continue;
            }
            List<String> coordinators = byTour.getOrDefault(tour.getId(), List.of()).stream()
                    .filter(o -> o.getDomainId().equals(domain.getId()) && o.getRole() == TeamRole.COORDINATOR)
                    .map(o -> people.containsKey(o.getUserId()) ? people.get(o.getUserId()).getFullName() : "Unknown")
                    .toList();
            jobs.add(new DomainJob(a.getId(), tour.getId(), tour.getVisitDate(), schoolName(tour.getId()),
                    domain.getName(), a.getRole(), jobsByDomain(tour.getId()).entrySet().stream()
                            .filter(e -> e.getKey().getId().equals(domain.getId()))
                            .flatMap(e -> e.getValue().stream()).toList(),
                    coordinators));
        }
        jobs.sort(Comparator.comparing(DomainJob::visitDate).thenComparing(DomainJob::domainName));
        return jobs;
    }

    // --- acting ------------------------------------------------------------------------

    @Transactional
    public void assign(Long programmeId, AssignMemberRequest request, AppUser actor) {
        Programme tour = programmes.findById(programmeId)
                .orElseThrow(() -> new NoSuchElementException("Tour request " + programmeId + " not found"));
        if (tour.getStatus() != ProgrammeStatus.APPROVED) {
            throw new IllegalStateException("A team can only be set up for an approved tour.");
        }
        if (tour.getVisitDate().isBefore(LocalDate.now(CAMPUS_ZONE))) {
            throw new IllegalStateException("This tour has already taken place, so its team can't be changed.");
        }
        Domain domain = jobsByDomain(programmeId).keySet().stream()
                .filter(d -> d.getId().equals(request.domainId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That domain has no job on this tour."));
        if (!canAssign(actor, domain, request.role(), programmeId)) {
            throw new AccessDeniedException(request.role() == TeamRole.COORDINATOR
                    ? "Only a lead, the faculty in-charge or the domain head can assign a coordinator."
                    : "Only a lead, the faculty in-charge, the domain head or this tour's coordinator can assign volunteers.");
        }
        AppUser person = candidates(actor, domain, request.role()).stream()
                .filter(u -> u.getId().equals(request.userId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(request.role() == TeamRole.COORDINATOR
                        ? "That person isn't an active coordinator of " + domain.getName() + "."
                        : "That person isn't an active volunteer of " + domain.getName() + "."));
        if (assignments.existsByProgrammeIdAndDomainIdAndUserId(programmeId, domain.getId(), person.getId())) {
            throw new IllegalStateException(person.getFullName() + " is already on this team.");
        }

        assignments.save(new DomainAssignment(programmeId, domain.getId(), person.getId(), request.role(), actor.getId()));
        auditLogs.save(AuditLog.byUser(actor.getId(), "TEAM_ASSIGNED", "Programme", programmeId,
                Map.of("userId", person.getId(), "domain", domain.getCode(), "role", request.role().name())));
    }

    @Transactional
    public void remove(Long assignmentId, AppUser actor) {
        DomainAssignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> new NoSuchElementException("Team assignment " + assignmentId + " not found"));
        Domain domain = domains.findById(assignment.getDomainId())
                .orElseThrow(() -> new NoSuchElementException("Domain " + assignment.getDomainId() + " not found"));
        if (!canAssign(actor, domain, assignment.getRole(), assignment.getProgrammeId())) {
            throw new AccessDeniedException("You can't remove this person from the team.");
        }
        assignments.delete(assignment);
        auditLogs.save(AuditLog.byUser(actor.getId(), "TEAM_REMOVED", "Programme", assignment.getProgrammeId(),
                Map.of("userId", assignment.getUserId(), "domain", domain.getCode(), "role", assignment.getRole().name())));
    }

    // --- helpers ---------------------------------------------------------------------------

    /** The domains that have a job on the tour, each with what the jobs are, in domain-name order. */
    private Map<Domain, List<String>> jobsByDomain(Long programmeId) {
        Map<Domain, List<String>> result = new TreeMap<>(Comparator.comparing(Domain::getName));
        for (Requirement requirement : requirements.findByProgrammeId(programmeId)) {
            result.computeIfAbsent(requirement.getDomain(), d -> new ArrayList<>()).add(label(requirement.getKind()));
        }
        return result;
    }

    private static String label(RequirementKind kind) {
        return switch (kind) {
            case LUNCH -> "Lunch";
            case VENUE_APPROVALS -> "Venue approvals";
            case VOLUNTEERS -> "Volunteers";
        };
    }

    private String schoolName(Long programmeId) {
        return tourDetails.findByProgrammeId(programmeId).map(d -> d.getSchool().getName()).orElse("(no details)");
    }

    private static List<TeamMember> members(List<DomainAssignment> rows, TeamRole role, Map<Long, AppUser> people) {
        return rows.stream().filter(a -> a.getRole() == role)
                .map(a -> new TeamMember(a.getId(), a.getUserId(),
                        people.containsKey(a.getUserId()) ? people.get(a.getUserId()).getFullName() : "Unknown"))
                .toList();
    }

    private List<UserOption> options(AppUser actor, Domain domain, TeamRole role, Set<Long> alreadyOn,
                                     Map<String, List<AppUser>> cache) {
        return cache.computeIfAbsent(domain.getId() + ":" + role, k -> candidates(actor, domain, role)).stream()
                .filter(u -> !alreadyOn.contains(u.getId()))
                .map(u -> new UserOption(u.getId(), u.getFullName()))
                .toList();
    }
}
