package com.prayas.platform.tour;

import com.prayas.platform.audit.AuditLog;
import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.domain.DomainRepository;
import com.prayas.platform.requirement.Requirement;
import com.prayas.platform.requirement.RequirementRepository;
import com.prayas.platform.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
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
 * A school asking to move its visit: the request waits until staff answer it,
 * nothing changes before that, and moving an already-approved tour obeys the
 * daily cap and clears the planning built around the old date. No database.
 */
class TourDecisionServiceDateChangeTest {

    private static final LocalDate OLD_DATE = LocalDate.now().plusDays(10);
    private static final LocalDate NEW_DATE = LocalDate.now().plusDays(20);

    private ProgrammeRepository programmes;
    private RequirementRepository requirements;
    private AuditLogRepository audit;
    private AdvisoryLockDao locks;
    private ApplicationEventPublisher events;
    private TourDecisionService service;
    private final AppUser staff = new AppUser("lead@iith.ac.in", "A Lead");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(staff, "id", 1L);
        programmes = mock(ProgrammeRepository.class);
        requirements = mock(RequirementRepository.class);
        audit = mock(AuditLogRepository.class);
        locks = mock(AdvisoryLockDao.class);
        events = mock(ApplicationEventPublisher.class);
        service = new TourDecisionService(programmes, mock(CampusTourDetailsRepository.class),
                mock(HeadcountRevisionRepository.class), mock(TourInterestRepository.class),
                requirements, mock(DomainRepository.class), audit, locks, events, new TourProperties(),
                mock(TrackingTokenGenerator.class), mock(TourTeacherRepository.class));
    }

    private Programme tour(ProgrammeStatus status, boolean pendingChange) {
        Programme programme = new Programme(OLD_DATE);
        programme.setArrivalTime(LocalTime.of(10, 0));
        programme.setDepartureTime(LocalTime.of(14, 0));
        programme.transitionTo(ProgrammeStatus.UNDER_REVIEW);
        if (status == ProgrammeStatus.APPROVED) {
            programme.transitionTo(ProgrammeStatus.APPROVED);
        } else if (status == ProgrammeStatus.CANCELLED) {
            programme.transitionTo(ProgrammeStatus.CANCELLED);
        }
        if (pendingChange) {
            programme.requestDateChange(NEW_DATE, LocalTime.of(11, 0), LocalTime.of(15, 0));
        }
        when(programmes.findById(5L)).thenReturn(Optional.of(programme));
        return programme;
    }

    private static DateChangeDecisionRequest approve() {
        return new DateChangeDecisionRequest(DateChangeDecisionRequest.Decision.APPROVE, null);
    }

    private static DateChangeDecisionRequest decline(String note) {
        return new DateChangeDecisionRequest(DateChangeDecisionRequest.Decision.DECLINE, note);
    }

    @Test
    void askingForANewDateChangesNothingUntilStaffApprove() {
        Programme programme = tour(ProgrammeStatus.UNDER_REVIEW, true);

        assertThat(programme.hasPendingDateChange()).isTrue();
        assertThat(programme.getVisitDate()).isEqualTo(OLD_DATE);
        assertThat(programme.getArrivalTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(programme.getRequestedVisitDate()).isEqualTo(NEW_DATE);
    }

    @Test
    void approvingMovesTheDateAndTimesAndClearsTheWaitingRequest() {
        Programme programme = tour(ProgrammeStatus.UNDER_REVIEW, true);

        service.resolveDateChange(5L, approve(), staff);

        assertThat(programme.getVisitDate()).isEqualTo(NEW_DATE);
        assertThat(programme.getArrivalTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(programme.getDepartureTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(programme.hasPendingDateChange()).isFalse();
        verify(audit).save(any(AuditLog.class));
        // nothing was planned yet for a tour that is still under review
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void decliningKeepsTheOldDateAndLeavesTheReasonForTheSchool() {
        Programme programme = tour(ProgrammeStatus.UNDER_REVIEW, true);

        service.resolveDateChange(5L, decline("  That week is exams  "), staff);

        assertThat(programme.getVisitDate()).isEqualTo(OLD_DATE);
        assertThat(programme.hasPendingDateChange()).isFalse();
        assertThat(programme.getDateChangeNote()).isEqualTo("That week is exams");
        verify(audit).save(any(AuditLog.class));
    }

    @Test
    void thereMustBeARequestWaitingToAnswerIt() {
        tour(ProgrammeStatus.UNDER_REVIEW, false);

        assertThatThrownBy(() -> service.resolveDateChange(5L, approve(), staff))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no date change");
    }

    @Test
    void aCancelledTourCannotHaveItsDateChangeAnswered() {
        Programme programme = tour(ProgrammeStatus.CANCELLED, true);

        assertThatThrownBy(() -> service.resolveDateChange(5L, approve(), staff))
                .isInstanceOf(IllegalStateException.class);
        assertThat(programme.getVisitDate()).isEqualTo(OLD_DATE);
    }

    @Test
    void aRequestedDateThatHasAlreadyPassedCannotBeApproved() {
        Programme programme = tour(ProgrammeStatus.UNDER_REVIEW, false);
        programme.requestDateChange(LocalDate.now().minusDays(1), LocalTime.of(10, 0), LocalTime.of(14, 0));

        assertThatThrownBy(() -> service.resolveDateChange(5L, approve(), staff))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already passed");
        assertThat(programme.getVisitDate()).isEqualTo(OLD_DATE);
    }

    @Test
    void movingAnApprovedTourTakesTheNewDateLockReschedulesRequirementsAndAnnouncesTheMove() {
        Programme programme = tour(ProgrammeStatus.APPROVED, true);
        Requirement requirement = mock(Requirement.class);
        when(requirements.findByProgrammeId(5L)).thenReturn(List.of(requirement));
        when(programmes.countApprovedOnDate(NEW_DATE)).thenReturn(1L);

        service.resolveDateChange(5L, approve(), staff);

        verify(locks).lockDate(NEW_DATE);
        assertThat(programme.getVisitDate()).isEqualTo(NEW_DATE);
        verify(requirement).rescheduleTo(NEW_DATE.minusDays(new TourProperties().getRequirementLeadDays()));
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new TourDateChangedEvent(5L, 1L));
    }

    @Test
    void anApprovedTourCannotMoveOntoADayThatIsAlreadyFull() {
        Programme programme = tour(ProgrammeStatus.APPROVED, true);
        when(programmes.countApprovedOnDate(NEW_DATE)).thenReturn((long) new TourProperties().getMaxPerDay());

        assertThatThrownBy(() -> service.resolveDateChange(5L, approve(), staff))
                .isInstanceOf(DailyCapExceededException.class);
        assertThat(programme.getVisitDate()).isEqualTo(OLD_DATE);
        assertThat(programme.hasPendingDateChange()).isTrue();
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void aTourWithAWaitingDateChangeCannotBeApprovedYet() {
        tour(ProgrammeStatus.UNDER_REVIEW, true);

        assertThatThrownBy(() -> service.decide(5L,
                new DecisionRequest(DecisionType.APPROVE, null, null), staff))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different date");
    }
}
