package com.prayas.platform.approval;

import com.prayas.platform.audit.AuditLog;
import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.authority.AuthorityContact;
import com.prayas.platform.authority.AuthorityContactRepository;
import com.prayas.platform.requirement.Requirement;
import com.prayas.platform.requirement.RequirementKind;
import com.prayas.platform.requirement.RequirementRepository;
import com.prayas.platform.requirement.RequirementStatus;
import com.prayas.platform.tour.CampusTourDetails;
import com.prayas.platform.tour.CampusTourDetailsRepository;
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
import com.prayas.platform.venue.VenueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Venue and mess approvals: which steps a tour gets, how the lunch chain
 * opens and closes, how steps roll up into the team's task status, and what
 * happens when the tour's date moves. Pure unit tests -- no database.
 */
class ApprovalServiceTest {

    private static final Long TOUR = 5L;

    private ApprovalStepRepository steps;
    private ProgrammeRepository programmes;
    private CampusTourDetailsRepository tourDetails;
    private TourInterestRepository interests;
    private AuthorityContactRepository contacts;
    private RequirementRepository requirements;
    private AuditLogRepository audit;
    private ApprovalService service;

    private final AppUser actor = user(1L);
    private final Requirement lunchRequirement = mock(Requirement.class);
    private final Requirement venueRequirement = mock(Requirement.class);
    private final List<ApprovalStep> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        steps = mock(ApprovalStepRepository.class);
        programmes = mock(ProgrammeRepository.class);
        tourDetails = mock(CampusTourDetailsRepository.class);
        interests = mock(TourInterestRepository.class);
        contacts = mock(AuthorityContactRepository.class);
        requirements = mock(RequirementRepository.class);
        audit = mock(AuditLogRepository.class);
        service = new ApprovalService(steps, programmes, tourDetails, mock(HeadcountRevisionRepository.class),
                interests, mock(VenueRepository.class), contacts, mock(AppUserRepository.class),
                requirements, audit);

        when(steps.save(any(ApprovalStep.class))).thenAnswer(inv -> inv.getArgument(0));
        when(steps.findByProgrammeIdOrderBySequenceAscIdAsc(TOUR)).thenReturn(stored);
        when(programmes.findById(TOUR)).thenReturn(Optional.of(approvedTour()));
        when(requirements.findByProgrammeIdAndKind(TOUR, RequirementKind.LUNCH)).thenReturn(Optional.of(lunchRequirement));
        when(requirements.findByProgrammeIdAndKind(TOUR, RequirementKind.VENUE_APPROVALS))
                .thenReturn(Optional.of(venueRequirement));
    }

    // --- opening the steps when a tour is approved ---------------------------

    @Test
    void approvingATourWithAnApprovalVenueAndLunchOpensThreeSteps() {
        Venue lab = new Venue("Physics Lab", VenueType.LAB);
        ReflectionTestUtils.setField(lab, "id", 30L);
        AuthorityContact head = new AuthorityContact("Head, Physics");
        ReflectionTestUtils.setField(head, "id", 40L);
        lab.setAuthorityContact(head);
        when(interests.venuesNeedingApproval(TOUR)).thenReturn(List.of(lab));
        CampusTourDetails details = mock(CampusTourDetails.class);
        when(details.isLunchRequired()).thenReturn(true);
        when(tourDetails.findByProgrammeId(TOUR)).thenReturn(Optional.of(details));

        service.onTourApproved(new TourApprovedEvent(TOUR));

        ArgumentCaptor<ApprovalStep> saved = ArgumentCaptor.forClass(ApprovalStep.class);
        verify(steps, org.mockito.Mockito.times(3)).save(saved.capture());
        List<ApprovalStep> created = saved.getAllValues();
        assertThat(created).extracting(ApprovalStep::getKind)
                .containsExactly(ApprovalKind.VENUE, ApprovalKind.HOSTEL_OFFICE, ApprovalKind.MESS);
        assertThat(created).extracting(ApprovalStep::getStatus)
                .containsExactly(ApprovalStatus.PENDING, ApprovalStatus.PENDING, ApprovalStatus.WAITING);
        // the venue step starts with the venue's own approver filled in
        assertThat(created.get(0).getAuthorityContactId()).isEqualTo(40L);
        assertThat(created.get(0).getVenueId()).isEqualTo(30L);
    }

    @Test
    void aTourWithNoApprovalVenueAndNoLunchNeedsNoSteps() {
        when(interests.venuesNeedingApproval(TOUR)).thenReturn(List.of());
        when(tourDetails.findByProgrammeId(TOUR)).thenReturn(Optional.empty());

        service.onTourApproved(new TourApprovedEvent(TOUR));

        verify(steps, never()).save(any(ApprovalStep.class));
    }

    @Test
    void openingTheStepsTwiceDoesNotCreateDuplicates() {
        stored.add(step(1L, ApprovalKind.HOSTEL_OFFICE, null, 1, ApprovalStatus.APPROVED));
        stored.add(step(2L, ApprovalKind.MESS, null, 2, ApprovalStatus.PENDING));
        when(interests.venuesNeedingApproval(TOUR)).thenReturn(List.of());
        CampusTourDetails details = mock(CampusTourDetails.class);
        when(details.isLunchRequired()).thenReturn(true);
        when(tourDetails.findByProgrammeId(TOUR)).thenReturn(Optional.of(details));

        service.onTourApproved(new TourApprovedEvent(TOUR));

        verify(steps, never()).save(any(ApprovalStep.class));
    }

    // --- recording progress ------------------------------------------------------

    @Test
    void contactingTheAuthorityMovesAStepToRequested() {
        ApprovalStep venue = add(step(1L, ApprovalKind.VENUE, 30L, 1, ApprovalStatus.PENDING));

        service.update(1L, new ApprovalUpdateRequest(ApprovalStatus.REQUESTED, "Called, they will reply Monday", null), actor);

        assertThat(venue.getStatus()).isEqualTo(ApprovalStatus.REQUESTED);
        assertThat(venue.getNote()).isEqualTo("Called, they will reply Monday");
        // one venue contacted and none answered yet: the task is under way
        verify(venueRequirement).syncStatus(RequirementStatus.IN_PROGRESS, true);
        verify(audit).save(any(AuditLog.class));
    }

    @Test
    void aDenialMustSayWhy() {
        add(step(1L, ApprovalKind.VENUE, 30L, 1, ApprovalStatus.REQUESTED));

        assertThatThrownBy(() -> service.update(1L, new ApprovalUpdateRequest(ApprovalStatus.DENIED, "  ", null), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("why");
    }

    @Test
    void aDeniedVenueBlocksTheVenueApprovalsTask() {
        add(step(1L, ApprovalKind.VENUE, 30L, 1, ApprovalStatus.REQUESTED));
        add(step(2L, ApprovalKind.VENUE, 31L, 1, ApprovalStatus.APPROVED));

        service.update(1L, new ApprovalUpdateRequest(ApprovalStatus.DENIED, "Lab under maintenance", null), actor);

        verify(venueRequirement).syncStatus(RequirementStatus.BLOCKED, true);
    }

    @Test
    void whenEveryVenueIsApprovedTheTaskIsDone() {
        add(step(1L, ApprovalKind.VENUE, 30L, 1, ApprovalStatus.REQUESTED));
        add(step(2L, ApprovalKind.VENUE, 31L, 1, ApprovalStatus.APPROVED));

        service.update(1L, new ApprovalUpdateRequest(ApprovalStatus.APPROVED, null, null), actor);

        verify(venueRequirement).syncStatus(RequirementStatus.DONE, true);
    }

    @Test
    void aStepThatHasNotOpenedYetCannotBeUpdated() {
        add(step(1L, ApprovalKind.HOSTEL_OFFICE, null, 1, ApprovalStatus.PENDING));
        add(step(2L, ApprovalKind.MESS, null, 2, ApprovalStatus.WAITING));

        assertThatThrownBy(() -> service.update(2L, new ApprovalUpdateRequest(ApprovalStatus.APPROVED, null, null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("opens once");
    }

    @Test
    void aStepCannotBeSetBackToWaitingByHand() {
        add(step(1L, ApprovalKind.VENUE, 30L, 1, ApprovalStatus.PENDING));

        assertThatThrownBy(() -> service.update(1L, new ApprovalUpdateRequest(ApprovalStatus.WAITING, null, null), actor))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approvalsCanOnlyBeRecordedForAnApprovedTour() {
        add(step(1L, ApprovalKind.VENUE, 30L, 1, ApprovalStatus.PENDING));
        Programme cancelled = new Programme(LocalDate.now().plusDays(10));
        cancelled.transitionTo(ProgrammeStatus.UNDER_REVIEW);
        cancelled.transitionTo(ProgrammeStatus.CANCELLED);
        when(programmes.findById(TOUR)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.update(1L, new ApprovalUpdateRequest(ApprovalStatus.APPROVED, null, null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved tour");
    }

    @Test
    void anUnknownContactIsRefused() {
        add(step(1L, ApprovalKind.VENUE, 30L, 1, ApprovalStatus.PENDING));
        when(contacts.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L,
                new ApprovalUpdateRequest(ApprovalStatus.REQUESTED, null, 99L), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("directory");
    }

    // --- the lunch chain --------------------------------------------------------

    @Test
    void approvingTheHostelOfficeOpensTheMessStep() {
        add(step(1L, ApprovalKind.HOSTEL_OFFICE, null, 1, ApprovalStatus.REQUESTED));
        ApprovalStep mess = add(step(2L, ApprovalKind.MESS, null, 2, ApprovalStatus.WAITING));

        service.update(1L, new ApprovalUpdateRequest(ApprovalStatus.APPROVED, null, null), actor);

        assertThat(mess.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        verify(lunchRequirement).syncStatus(RequirementStatus.IN_PROGRESS, true);
    }

    @Test
    void lunchIsDoneOnlyWhenBothStepsAreApproved() {
        add(step(1L, ApprovalKind.HOSTEL_OFFICE, null, 1, ApprovalStatus.APPROVED));
        add(step(2L, ApprovalKind.MESS, null, 2, ApprovalStatus.REQUESTED));

        service.update(2L, new ApprovalUpdateRequest(ApprovalStatus.APPROVED, null, null), actor);

        verify(lunchRequirement).syncStatus(RequirementStatus.DONE, true);
    }

    @Test
    void undoingTheHostelApprovalClosesTheMessStepAndClearsIt() {
        ApprovalStep hostel = add(step(1L, ApprovalKind.HOSTEL_OFFICE, null, 1, ApprovalStatus.APPROVED));
        ApprovalStep mess = add(step(2L, ApprovalKind.MESS, null, 2, ApprovalStatus.APPROVED));

        service.update(1L, new ApprovalUpdateRequest(ApprovalStatus.PENDING, null, null), actor);

        assertThat(hostel.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(mess.getStatus()).isEqualTo(ApprovalStatus.WAITING);
        assertThat(mess.getDecidedBy() == null).isTrue();
        verify(lunchRequirement).syncStatus(RequirementStatus.PENDING, true);
    }

    // --- the date moves ------------------------------------------------------------

    @Test
    void whenTheTourDateMovesEveryAnsweredStepGoesBackToTheStart() {
        ApprovalStep venue = add(step(1L, ApprovalKind.VENUE, 30L, 1, ApprovalStatus.APPROVED));
        ApprovalStep hostel = add(step(2L, ApprovalKind.HOSTEL_OFFICE, null, 1, ApprovalStatus.APPROVED));
        ApprovalStep mess = add(step(3L, ApprovalKind.MESS, null, 2, ApprovalStatus.REQUESTED));

        service.onTourDateChanged(new TourDateChangedEvent(TOUR, 1L));

        assertThat(venue.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(hostel.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(mess.getStatus()).isEqualTo(ApprovalStatus.WAITING);
        // the team is not told "nothing changed": the flag stays raised
        verify(venueRequirement).syncStatus(RequirementStatus.PENDING, false);
        verify(lunchRequirement).syncStatus(RequirementStatus.PENDING, false);
        verify(audit).save(any(AuditLog.class));
    }

    @Test
    void aDateMoveWithNothingAnsweredYetChangesNothingAndIsNotAudited() {
        add(step(1L, ApprovalKind.VENUE, 30L, 1, ApprovalStatus.PENDING));

        service.onTourDateChanged(new TourDateChangedEvent(TOUR, 1L));

        verify(audit, never()).save(any(AuditLog.class));
    }

    // --- how steps roll up -----------------------------------------------------------

    @Test
    void stepsRollUpIntoOneTaskStatus() {
        assertThat(ApprovalService.rollUp(List.of())).isEqualTo(RequirementStatus.PENDING);
        assertThat(ApprovalService.rollUp(List.of(
                step(1L, ApprovalKind.VENUE, 1L, 1, ApprovalStatus.PENDING)))).isEqualTo(RequirementStatus.PENDING);
        assertThat(ApprovalService.rollUp(List.of(
                step(1L, ApprovalKind.VENUE, 1L, 1, ApprovalStatus.REQUESTED)))).isEqualTo(RequirementStatus.IN_PROGRESS);
        assertThat(ApprovalService.rollUp(List.of(
                step(1L, ApprovalKind.VENUE, 1L, 1, ApprovalStatus.APPROVED),
                step(2L, ApprovalKind.VENUE, 2L, 1, ApprovalStatus.PENDING)))).isEqualTo(RequirementStatus.IN_PROGRESS);
        assertThat(ApprovalService.rollUp(List.of(
                step(1L, ApprovalKind.VENUE, 1L, 1, ApprovalStatus.APPROVED),
                step(2L, ApprovalKind.VENUE, 2L, 1, ApprovalStatus.DENIED)))).isEqualTo(RequirementStatus.BLOCKED);
        assertThat(ApprovalService.rollUp(List.of(
                step(1L, ApprovalKind.VENUE, 1L, 1, ApprovalStatus.APPROVED)))).isEqualTo(RequirementStatus.DONE);
    }

    // --- helpers -------------------------------------------------------------------------

    private ApprovalStep add(ApprovalStep step) {
        stored.add(step);
        when(steps.findById(step.getId())).thenReturn(Optional.of(step));
        return step;
    }

    private static ApprovalStep step(Long id, ApprovalKind kind, Long venueId, int sequence, ApprovalStatus status) {
        ApprovalStep step = new ApprovalStep(TOUR, kind, venueId, null, sequence, status);
        ReflectionTestUtils.setField(step, "id", id);
        return step;
    }

    private static Programme approvedTour() {
        Programme programme = new Programme(LocalDate.now().plusDays(10));
        programme.transitionTo(ProgrammeStatus.UNDER_REVIEW);
        programme.transitionTo(ProgrammeStatus.APPROVED);
        return programme;
    }

    private static AppUser user(Long id) {
        AppUser user = new AppUser("h.l@iith.ac.in", "H L Member");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
