package com.prayas.platform.team;

import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.domain.Domain;
import com.prayas.platform.domain.DomainMembershipRepository;
import com.prayas.platform.domain.DomainRepository;
import com.prayas.platform.domain.DomainRole;
import com.prayas.platform.requirement.Requirement;
import com.prayas.platform.requirement.RequirementKind;
import com.prayas.platform.requirement.RequirementRepository;
import com.prayas.platform.team.TeamViews.TourTeams;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.tour.ProgrammeRepository;
import com.prayas.platform.tour.ProgrammeStatus;
import com.prayas.platform.tour.TourDateChangedEvent;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import com.prayas.platform.user.GlobalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The chain of command: who may put whom on a domain's team for a tour. Pure unit tests -- no database. */
class DomainTeamServiceTest {

    private static final Long TOUR = 5L;
    private static final Long HL_ID = 10L;

    private DomainAssignmentRepository assignments;
    private ProgrammeRepository programmes;
    private RequirementRepository requirements;
    private DomainMembershipRepository memberships;
    private DomainRepository domains;
    private AppUserRepository users;
    private DomainTeamService service;

    private final Domain hl = domain(HL_ID, "HOSPITALITY_LOGISTICS", "Hospitality & Logistics");
    private final AppUser lead = user(1L, "Lead", GlobalRole.LEAD);
    private final AppUser faculty = user(2L, "Faculty", GlobalRole.FACULTY_INCHARGE);
    private final AppUser head = user(3L, "Head", GlobalRole.MEMBER);
    private final AppUser coordinator = user(4L, "Coordinator", GlobalRole.MEMBER);
    private final AppUser volunteer = user(5L, "Volunteer", GlobalRole.MEMBER);
    private final AppUser outsider = user(6L, "Outsider", GlobalRole.MEMBER);

    @BeforeEach
    void setUp() {
        assignments = mock(DomainAssignmentRepository.class);
        programmes = mock(ProgrammeRepository.class);
        requirements = mock(RequirementRepository.class);
        memberships = mock(DomainMembershipRepository.class);
        domains = mock(DomainRepository.class);
        users = mock(AppUserRepository.class);
        service = new DomainTeamService(assignments, programmes, requirements, memberships, domains, users,
                mock(CampusTourDetailsRepository.class), mock(AuditLogRepository.class));

        Requirement lunch = mock(Requirement.class);
        when(lunch.getDomain()).thenReturn(hl);
        when(lunch.getKind()).thenReturn(RequirementKind.LUNCH);
        when(requirements.findByProgrammeId(TOUR)).thenReturn(List.of(lunch));
        when(programmes.findById(TOUR)).thenReturn(Optional.of(tour(10)));
        when(domains.findById(HL_ID)).thenReturn(Optional.of(hl));

        when(memberships.existsByUserAndDomainCodeAndRole(3L, "HOSPITALITY_LOGISTICS", DomainRole.HEAD)).thenReturn(true);
        when(memberships.findUsersByDomainAndRole(HL_ID, DomainRole.COORDINATOR)).thenReturn(List.of(coordinator));
        when(memberships.findUsersByDomainAndRole(HL_ID, DomainRole.VOLUNTEER)).thenReturn(List.of(volunteer));
        when(users.findAll()).thenReturn(List.of(lead, faculty, head, coordinator, volunteer, outsider));
    }

    // --- assigning coordinators ------------------------------------------------------

    @Test
    void aLeadCanMakeAnyActivePersonACoordinator() {
        service.assign(TOUR, new AssignMemberRequest(HL_ID, outsider.getId(), TeamRole.COORDINATOR), lead);

        ArgumentCaptor<DomainAssignment> saved = ArgumentCaptor.forClass(DomainAssignment.class);
        verify(assignments).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(6L);
        assertThat(saved.getValue().getRole()).isEqualTo(TeamRole.COORDINATOR);
        assertThat(saved.getValue().getDomainId()).isEqualTo(HL_ID);
    }

    @Test
    void theFacultyInChargeCanAssignAnyoneToo() {
        service.assign(TOUR, new AssignMemberRequest(HL_ID, outsider.getId(), TeamRole.VOLUNTEER), faculty);

        verify(assignments).save(any(DomainAssignment.class));
    }

    @Test
    void aDomainHeadCanMakeOneOfTheDomainsCoordinatorsACoordinator() {
        service.assign(TOUR, new AssignMemberRequest(HL_ID, coordinator.getId(), TeamRole.COORDINATOR), head);

        verify(assignments).save(any(DomainAssignment.class));
    }

    @Test
    void aDomainHeadCannotPickSomeoneOutsideTheDomain() {
        assertThatThrownBy(() ->
                service.assign(TOUR, new AssignMemberRequest(HL_ID, outsider.getId(), TeamRole.COORDINATOR), head))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("isn't an active coordinator");
        verify(assignments, never()).save(any());
    }

    @Test
    void aCoordinatorCannotAssignAnotherCoordinator() {
        whenOnTeam(coordinator, TeamRole.COORDINATOR);

        assertThatThrownBy(() ->
                service.assign(TOUR, new AssignMemberRequest(HL_ID, coordinator.getId(), TeamRole.COORDINATOR), coordinator))
                .isInstanceOf(AccessDeniedException.class);
        verify(assignments, never()).save(any());
    }

    // --- assigning volunteers ----------------------------------------------------------

    @Test
    void aCoordinatorOnThisToursTeamCanAddTheDomainsVolunteers() {
        whenOnTeam(coordinator, TeamRole.COORDINATOR);

        service.assign(TOUR, new AssignMemberRequest(HL_ID, volunteer.getId(), TeamRole.VOLUNTEER), coordinator);

        verify(assignments).save(any(DomainAssignment.class));
    }

    @Test
    void aCoordinatorNotOnThisToursTeamCannotAddVolunteers() {
        assertThatThrownBy(() ->
                service.assign(TOUR, new AssignMemberRequest(HL_ID, volunteer.getId(), TeamRole.VOLUNTEER), coordinator))
                .isInstanceOf(AccessDeniedException.class);
        verify(assignments, never()).save(any());
    }

    @Test
    void aPlainVolunteerCannotAssignAnyone() {
        assertThatThrownBy(() ->
                service.assign(TOUR, new AssignMemberRequest(HL_ID, volunteer.getId(), TeamRole.VOLUNTEER), volunteer))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aCoordinatorOnlyPicksFromTheDomainsOwnVolunteers() {
        whenOnTeam(coordinator, TeamRole.COORDINATOR);

        assertThatThrownBy(() ->
                service.assign(TOUR, new AssignMemberRequest(HL_ID, outsider.getId(), TeamRole.VOLUNTEER), coordinator))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- rules that apply to everyone ---------------------------------------------------

    @Test
    void aPersonCannotBeAddedTwice() {
        when(assignments.existsByProgrammeIdAndDomainIdAndUserId(TOUR, HL_ID, 6L)).thenReturn(true);

        assertThatThrownBy(() ->
                service.assign(TOUR, new AssignMemberRequest(HL_ID, outsider.getId(), TeamRole.VOLUNTEER), lead))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already on this team");
    }

    @Test
    void aDomainWithNoJobOnTheTourCannotHaveATeam() {
        Domain web = domain(11L, "WEB", "Web");

        assertThatThrownBy(() ->
                service.assign(TOUR, new AssignMemberRequest(web.getId(), outsider.getId(), TeamRole.VOLUNTEER), lead))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no job on this tour");
    }

    @Test
    void aTourThatIsNotApprovedHasNoTeam() {
        Programme submitted = new Programme(LocalDate.now().plusDays(10));
        when(programmes.findById(TOUR)).thenReturn(Optional.of(submitted));

        assertThatThrownBy(() ->
                service.assign(TOUR, new AssignMemberRequest(HL_ID, outsider.getId(), TeamRole.VOLUNTEER), lead))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved tour");
    }

    @Test
    void aTourThatAlreadyHappenedCannotBeChanged() {
        when(programmes.findById(TOUR)).thenReturn(Optional.of(tour(-3)));

        assertThatThrownBy(() ->
                service.assign(TOUR, new AssignMemberRequest(HL_ID, outsider.getId(), TeamRole.VOLUNTEER), lead))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already taken place");
    }

    @Test
    void anUnknownTourIsNotFound() {
        when(programmes.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.assign(99L, new AssignMemberRequest(HL_ID, outsider.getId(), TeamRole.VOLUNTEER), lead))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // --- removing ------------------------------------------------------------------------

    @Test
    void aCoordinatorCanRemoveAVolunteerOnTheirTour() {
        whenOnTeam(coordinator, TeamRole.COORDINATOR);
        DomainAssignment row = new DomainAssignment(TOUR, HL_ID, volunteer.getId(), TeamRole.VOLUNTEER, 4L);
        when(assignments.findById(7L)).thenReturn(Optional.of(row));

        service.remove(7L, coordinator);

        verify(assignments).delete(row);
    }

    @Test
    void aCoordinatorCannotRemoveAnotherCoordinator() {
        whenOnTeam(coordinator, TeamRole.COORDINATOR);
        DomainAssignment row = new DomainAssignment(TOUR, HL_ID, 8L, TeamRole.COORDINATOR, 1L);
        when(assignments.findById(7L)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.remove(7L, coordinator)).isInstanceOf(AccessDeniedException.class);
        verify(assignments, never()).delete(any(DomainAssignment.class));
    }

    @Test
    void aLeadCanRemoveACoordinator() {
        DomainAssignment row = new DomainAssignment(TOUR, HL_ID, 8L, TeamRole.COORDINATOR, 1L);
        when(assignments.findById(7L)).thenReturn(Optional.of(row));

        service.remove(7L, lead);

        verify(assignments).delete(row);
    }

    // --- what the Teams page is told ----------------------------------------------------------

    @Test
    void theTeamsPageOffersAHeadOnlyCoordinatorsFromTheirDomain() {
        when(programmes.findByStatusAndVisitDateGreaterThanEqualOrderByVisitDateAscIdAsc(eq(ProgrammeStatus.APPROVED), any()))
                .thenReturn(List.of(tour(10)));
        when(assignments.findByProgrammeIdIn(any())).thenReturn(List.of());

        List<TourTeams> result = service.teams(head);

        TeamViews.DomainTeam team = result.get(0).domains().get(0);
        assertThat(team.canAssignCoordinator()).isTrue();
        assertThat(team.coordinatorOptions()).extracting(TeamViews.UserOption::name).containsExactly("Coordinator");
        assertThat(team.jobs()).containsExactly("Lunch");
    }

    @Test
    void aPlainVolunteerGetsNoControlsOrOptions() {
        when(programmes.findByStatusAndVisitDateGreaterThanEqualOrderByVisitDateAscIdAsc(eq(ProgrammeStatus.APPROVED), any()))
                .thenReturn(List.of(tour(10)));
        when(assignments.findByProgrammeIdIn(any())).thenReturn(List.of());

        TeamViews.DomainTeam team = service.teams(volunteer).get(0).domains().get(0);

        assertThat(team.canAssignCoordinator()).isFalse();
        assertThat(team.canAssignVolunteer()).isFalse();
        assertThat(team.coordinatorOptions()).isEmpty();
        assertThat(team.volunteerOptions()).isEmpty();
    }

    @Test
    void peopleAlreadyOnTheTeamAreNotOfferedAgain() {
        when(programmes.findByStatusAndVisitDateGreaterThanEqualOrderByVisitDateAscIdAsc(eq(ProgrammeStatus.APPROVED), any()))
                .thenReturn(List.of(tour(10)));
        DomainAssignment onTeam = new DomainAssignment(TOUR, HL_ID, coordinator.getId(), TeamRole.COORDINATOR, 1L);
        when(assignments.findByProgrammeIdIn(any())).thenReturn(List.of(onTeam));
        when(users.findAllById(any())).thenReturn(List.of(coordinator));

        TeamViews.DomainTeam team = service.teams(lead).get(0).domains().get(0);

        assertThat(team.coordinators()).extracting(TeamViews.TeamMember::name).containsExactly("Coordinator");
        assertThat(team.coordinatorOptions()).extracting(TeamViews.UserOption::name).doesNotContain("Coordinator");
    }

    // --- helpers ------------------------------------------------------------------------------

    // --- ticking off your own part ---------------------------------------------------

    @Test
    void aPersonCanTickOffTheirOwnPartWithANoteAndALink() {
        DomainAssignment mine = myAssignment(volunteer);

        service.markDone(50L, new MarkDoneRequest("  Lunch packets delivered  ", " https://drive.example.com/photo "), volunteer);

        assertThat(mine.isDone()).isTrue();
        assertThat(mine.getCompletionNote()).isEqualTo("Lunch packets delivered");
        assertThat(mine.getProofUrl()).isEqualTo("https://drive.example.com/photo");
    }

    @Test
    void theNoteAndTheLinkAreOptional() {
        DomainAssignment mine = myAssignment(volunteer);

        service.markDone(50L, new MarkDoneRequest("  ", null), volunteer);

        assertThat(mine.isDone()).isTrue();
        assertThat(mine.getCompletionNote()).isNull();
        assertThat(mine.getProofUrl()).isNull();
    }

    @Test
    void aProofLinkThatIsNotAWebAddressIsRefused() {
        DomainAssignment mine = myAssignment(volunteer);

        assertThatThrownBy(() -> service.markDone(50L, new MarkDoneRequest(null, "javascript:alert(1)"), volunteer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http://");
        assertThatThrownBy(() -> service.markDone(50L, new MarkDoneRequest(null, "see my photo"), volunteer))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(mine.isDone()).isFalse();
    }

    @Test
    void nobodyCanTickOffSomeoneElsesPart() {
        DomainAssignment mine = myAssignment(volunteer);

        assertThatThrownBy(() -> service.markDone(50L, new MarkDoneRequest(null, null), outsider))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.undoDone(50L, lead))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(mine.isDone()).isFalse();
    }

    @Test
    void aTourThatAlreadyHappenedCannotBeTickedOff() {
        myAssignment(volunteer);
        when(programmes.findById(TOUR)).thenReturn(Optional.of(tour(-1)));

        assertThatThrownBy(() -> service.markDone(50L, new MarkDoneRequest(null, null), volunteer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already taken place");
    }

    @Test
    void takingTheTickBackClearsTheNoteAndLink() {
        DomainAssignment mine = myAssignment(volunteer);
        service.markDone(50L, new MarkDoneRequest("done", "https://example.com/x"), volunteer);

        service.undoDone(50L, volunteer);

        assertThat(mine.isDone()).isFalse();
        assertThat(mine.getCompletionNote()).isNull();
        assertThat(mine.getProofUrl()).isNull();
    }

    @Test
    void aNewVisitDateClearsEveryonesTick() {
        DomainAssignment first = new DomainAssignment(TOUR, HL_ID, 5L, TeamRole.VOLUNTEER, 1L);
        DomainAssignment second = new DomainAssignment(TOUR, HL_ID, 4L, TeamRole.COORDINATOR, 1L);
        first.markDone("ok", null);
        when(assignments.findByProgrammeIdIn(List.of(TOUR))).thenReturn(List.of(first, second));

        service.onTourDateChanged(new TourDateChangedEvent(TOUR, 1L));

        assertThat(first.isDone()).isFalse();
        assertThat(first.getCompletionNote()).isNull();
        assertThat(second.isDone()).isFalse();
    }

    @Test
    void myJobsShowWhetherEachPartIsDone() {
        DomainAssignment mine = myAssignment(volunteer);
        ReflectionTestUtils.setField(mine, "userId", 5L);
        mine.markDone("all set", "https://example.com/p");
        when(assignments.findByUserId(5L)).thenReturn(List.of(mine));
        when(assignments.findByProgrammeIdIn(any())).thenReturn(List.of(mine));
        when(programmes.findAllById(any())).thenReturn(List.of(tour(10)));
        when(domains.findAllById(any())).thenReturn(List.of(hl));
        when(users.findAllById(any())).thenReturn(List.of(volunteer));

        TeamViews.DomainJob job = service.myJobs(5L).get(0);

        assertThat(job.done()).isTrue();
        assertThat(job.note()).isEqualTo("all set");
        assertThat(job.proofUrl()).isEqualTo("https://example.com/p");
    }

    private DomainAssignment myAssignment(AppUser person) {
        DomainAssignment assignment = new DomainAssignment(TOUR, HL_ID, person.getId(), TeamRole.VOLUNTEER, 1L);
        ReflectionTestUtils.setField(assignment, "id", 50L);
        when(assignments.findById(50L)).thenReturn(Optional.of(assignment));
        return assignment;
    }

    private void whenOnTeam(AppUser person, TeamRole role) {
        when(assignments.existsByProgrammeIdAndDomainIdAndUserIdAndRole(TOUR, HL_ID, person.getId(), role))
                .thenReturn(true);
    }

    private static Programme tour(int daysFromToday) {
        Programme programme = new Programme(LocalDate.now().plusDays(daysFromToday));
        programme.transitionTo(ProgrammeStatus.UNDER_REVIEW);
        programme.transitionTo(ProgrammeStatus.APPROVED);
        ReflectionTestUtils.setField(programme, "id", TOUR);
        return programme;
    }

    private static Domain domain(Long id, String code, String name) {
        Domain domain = mock(Domain.class);
        when(domain.getId()).thenReturn(id);
        when(domain.getCode()).thenReturn(code);
        when(domain.getName()).thenReturn(name);
        return domain;
    }

    private static AppUser user(Long id, String name, GlobalRole role) {
        AppUser user = new AppUser(name.toLowerCase() + "@iith.ac.in", name);
        user.setGlobalRole(role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
