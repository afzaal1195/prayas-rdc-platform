package com.prayas.platform.taskstatus;

import com.prayas.platform.approval.ApprovalKind;
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
import com.prayas.platform.taskstatus.TaskStatusViews.DomainStatus;
import com.prayas.platform.taskstatus.TaskStatusViews.TaskBoard;
import com.prayas.platform.taskstatus.TaskStatusViews.TaskRow;
import com.prayas.platform.team.DomainAssignment;
import com.prayas.platform.team.DomainAssignmentRepository;
import com.prayas.platform.team.TeamRole;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.tour.ProgrammeRepository;
import com.prayas.platform.tour.ProgrammeStatus;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import com.prayas.platform.user.GlobalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The task status board: who sees which domains, the counts, and who may set a status by hand. No database. */
class TaskStatusServiceTest {

    private static final Long TOUR = 5L;

    private ProgrammeRepository programmes;
    private RequirementRepository requirements;
    private ApprovalStepRepository steps;
    private DomainMembershipRepository memberships;
    private DomainAssignmentRepository assignments;
    private AppUserRepository users;
    private TaskStatusService service;

    private final Domain hl = domain(10L, "HOSPITALITY_LOGISTICS", "Hospitality & Logistics");
    private final Domain vcu = domain(11L, "VCU", "Volunteer Coordination");
    private final AppUser lead = user(1L, "Lead", GlobalRole.LEAD);
    private final AppUser member = user(2L, "Member", GlobalRole.MEMBER);
    private final AppUser coordinator = user(3L, "Coordinator", GlobalRole.MEMBER);

    @BeforeEach
    void setUp() {
        programmes = mock(ProgrammeRepository.class);
        requirements = mock(RequirementRepository.class);
        steps = mock(ApprovalStepRepository.class);
        memberships = mock(DomainMembershipRepository.class);
        assignments = mock(DomainAssignmentRepository.class);
        users = mock(AppUserRepository.class);
        service = new TaskStatusService(programmes, requirements, steps, memberships, assignments, users,
                mock(CampusTourDetailsRepository.class));

        Programme tour = new Programme(LocalDate.now().plusDays(10));
        tour.transitionTo(ProgrammeStatus.UNDER_REVIEW);
        tour.transitionTo(ProgrammeStatus.APPROVED);
        ReflectionTestUtils.setField(tour, "id", TOUR);
        when(programmes.findByStatusAndVisitDateGreaterThanEqualOrderByVisitDateAscIdAsc(any(), any()))
                .thenReturn(List.of(tour));
        List<Requirement> tasks = List.of(
                task(hl, RequirementKind.LUNCH, RequirementStatus.IN_PROGRESS, false),
                task(hl, RequirementKind.VENUE_APPROVALS, RequirementStatus.DONE, false),
                task(vcu, RequirementKind.VOLUNTEERS, RequirementStatus.PENDING, true));
        when(requirements.findByProgrammeIds(any())).thenReturn(tasks);
    }

    @Test
    void aLeadSeesEveryDomainAndTotals() {
        TaskBoard board = service.board(lead);

        assertThat(board.overview()).isTrue();
        assertThat(board.domains()).extracting(DomainStatus::domainName)
                .containsExactly("Hospitality & Logistics", "Volunteer Coordination");
        assertThat(board.totals().total()).isEqualTo(3);
        assertThat(board.totals().done()).isEqualTo(1);
        assertThat(board.totals().inProgress()).isEqualTo(1);
        assertThat(board.totals().pending()).isEqualTo(1);
        assertThat(board.totals().needsAttention()).isEqualTo(1);
    }

    @Test
    void aMemberSeesOnlyTheirOwnDomains() {
        List<DomainMembership> mine = List.of(membership(member, vcu, DomainRole.VOLUNTEER));
        when(memberships.findByUserId(2L)).thenReturn(mine);

        TaskBoard board = service.board(member);

        assertThat(board.overview()).isFalse();
        assertThat(board.domains()).extracting(DomainStatus::domainCode).containsExactly("VCU");
        assertThat(board.totals().total()).isEqualTo(1);
    }

    @Test
    void somebodyInNoDomainSeesNothing() {
        TaskBoard board = service.board(member);

        assertThat(board.domains()).isEmpty();
        assertThat(board.totals().total()).isZero();
    }

    @Test
    void aVolunteerCannotSetStatusButACoordinatorCan() {
        List<DomainMembership> asVolunteer = List.of(membership(member, vcu, DomainRole.VOLUNTEER));
        List<DomainMembership> asCoordinator = List.of(membership(coordinator, vcu, DomainRole.COORDINATOR));
        when(memberships.findByUserId(2L)).thenReturn(asVolunteer);
        when(memberships.findByUserId(3L)).thenReturn(asCoordinator);

        assertThat(row(service.board(member), "VCU").canSetStatus()).isFalse();
        assertThat(row(service.board(coordinator), "VCU").canSetStatus()).isTrue();
        assertThat(row(service.board(lead), "VCU").canSetStatus()).isTrue();
    }

    @Test
    void tasksWorkedOutFromApprovalStepsCannotBeSetByHand() {
        TaskRow lunch = service.board(lead).domains().get(0).tasks().stream()
                .filter(t -> t.kind() == RequirementKind.LUNCH).findFirst().orElseThrow();

        assertThat(lunch.canSetStatus()).isFalse();
    }

    @Test
    void lunchShowsHowManyOfItsApprovalsAreDone() {
        ApprovalStep hostel = step(ApprovalKind.HOSTEL_OFFICE, ApprovalStatus.APPROVED);
        ApprovalStep mess = step(ApprovalKind.MESS, ApprovalStatus.PENDING);
        when(steps.findForUpcomingTours(any(), any())).thenReturn(List.of(hostel, mess));

        TaskRow lunch = service.board(lead).domains().get(0).tasks().stream()
                .filter(t -> t.kind() == RequirementKind.LUNCH).findFirst().orElseThrow();

        assertThat(lunch.progress()).isEqualTo("1 of 2 approvals done");
    }

    @Test
    void theDomainsTeamIsListedOnItsTasks() {
        when(assignments.findByProgrammeIdIn(any())).thenReturn(List.of(
                new DomainAssignment(TOUR, 11L, 3L, TeamRole.COORDINATOR, 1L),
                new DomainAssignment(TOUR, 11L, 2L, TeamRole.VOLUNTEER, 3L)));
        when(users.findAllById(any())).thenReturn(List.of(coordinator, member));

        TaskRow volunteers = row(service.board(lead), "VCU");

        assertThat(volunteers.coordinators()).containsExactly("Coordinator");
        assertThat(volunteers.volunteers()).containsExactly("Member");
    }

    @Test
    void theBoardShowsWhoHasFinishedTheirPart() {
        DomainAssignment volunteerDone = new DomainAssignment(TOUR, 11L, 2L, TeamRole.VOLUNTEER, 3L);
        volunteerDone.markDone("All collected", "https://example.com/proof");
        DomainAssignment coordinatorPending = new DomainAssignment(TOUR, 11L, 3L, TeamRole.COORDINATOR, 1L);
        when(assignments.findByProgrammeIdIn(any())).thenReturn(List.of(volunteerDone, coordinatorPending));
        when(users.findAllById(any())).thenReturn(List.of(coordinator, member));

        TaskRow volunteers = row(service.board(lead), "VCU");

        assertThat(volunteers.members()).hasSize(2);
        assertThat(volunteers.members()).extracting(TaskStatusViews.MemberStatus::name)
                .containsExactly("Coordinator", "Member");
        TaskStatusViews.MemberStatus finished = volunteers.members().get(1);
        assertThat(finished.done()).isTrue();
        assertThat(finished.note()).isEqualTo("All collected");
        assertThat(finished.proofUrl()).isEqualTo("https://example.com/proof");
        assertThat(volunteers.members().get(0).done()).isFalse();
    }

    @Test
    void cancelledTasksAreLeftOut() {
        List<Requirement> cancelled = List.of(task(hl, RequirementKind.LUNCH, RequirementStatus.CANCELLED, false));
        when(requirements.findByProgrammeIds(any())).thenReturn(cancelled);

        assertThat(service.board(lead).totals().total()).isZero();
    }

    // --- helpers ------------------------------------------------------------------

    private static TaskRow row(TaskBoard board, String domainCode) {
        return board.domains().stream().filter(d -> d.domainCode().equals(domainCode)).findFirst().orElseThrow()
                .tasks().get(0);
    }

    private static Requirement task(Domain domain, RequirementKind kind, RequirementStatus status, boolean attention) {
        Requirement r = mock(Requirement.class);
        when(r.getDomain()).thenReturn(domain);
        when(r.getKind()).thenReturn(kind);
        when(r.getStatus()).thenReturn(status);
        when(r.needsAttention()).thenReturn(attention);
        when(r.getProgrammeId()).thenReturn(TOUR);
        when(r.getDueDate()).thenReturn(LocalDate.now().plusDays(5));
        return r;
    }

    private static ApprovalStep step(ApprovalKind kind, ApprovalStatus status) {
        ApprovalStep s = mock(ApprovalStep.class);
        when(s.getProgrammeId()).thenReturn(TOUR);
        when(s.getKind()).thenReturn(kind);
        when(s.getStatus()).thenReturn(status);
        return s;
    }

    private static DomainMembership membership(AppUser user, Domain domain, DomainRole role) {
        DomainMembership m = mock(DomainMembership.class);
        when(m.getDomain()).thenReturn(domain);
        when(m.getDomainRole()).thenReturn(role);
        return m;
    }

    private static Domain domain(Long id, String code, String name) {
        Domain d = mock(Domain.class);
        when(d.getId()).thenReturn(id);
        when(d.getCode()).thenReturn(code);
        when(d.getName()).thenReturn(name);
        return d;
    }

    private static AppUser user(Long id, String name, GlobalRole role) {
        AppUser u = new AppUser(name.toLowerCase() + "@iith.ac.in", name);
        u.setGlobalRole(role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}
