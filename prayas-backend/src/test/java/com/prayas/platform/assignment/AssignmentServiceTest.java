package com.prayas.platform.assignment;

import com.prayas.platform.audit.AuditLog;
import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.domain.DomainRole;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.HeadcountRevisionRepository;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.tour.ProgrammeRepository;
import com.prayas.platform.tour.ProgrammeStatus;
import com.prayas.platform.tour.TourDateChangedEvent;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The shift rules: who can be put on a tour, no double-booking, and how a
 * hand-over splits a shift. Pure unit tests with mocked repositories -- the
 * database-level guarantee (no_overlapping_shifts) is covered by how
 * {@code DataIntegrityViolationException} is turned into a readable error.
 */
class AssignmentServiceTest {

    private static final ZoneId CAMPUS = ZoneId.of("Asia/Kolkata");
    private static final LocalDate VISIT = LocalDate.now(CAMPUS).plusDays(10);

    private TourAssignmentRepository assignments;
    private ProgrammeRepository programmes;
    private AppUserRepository users;
    private AuditLogRepository audit;
    private AssignmentService service;

    private final AppUser actor = user(1L, "Lead");
    private final AppUser volunteerA = user(10L, "Volunteer A");
    private final AppUser volunteerB = user(11L, "Volunteer B");
    private final AppUser coordinatorC = user(20L, "Coordinator C");
    private final AppUser coordinatorD = user(21L, "Coordinator D");

    @BeforeEach
    void setUp() {
        assignments = mock(TourAssignmentRepository.class);
        programmes = mock(ProgrammeRepository.class);
        users = mock(AppUserRepository.class);
        audit = mock(AuditLogRepository.class);
        service = new AssignmentService(assignments, programmes, users, audit,
                mock(CampusTourDetailsRepository.class), mock(HeadcountRevisionRepository.class));

        when(assignments.saveAndFlush(any(TourAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(users.findEligible(List.of(DomainRole.VOLUNTEER), true)).thenReturn(List.of(volunteerA, volunteerB));
        when(users.findEligible(List.of(DomainRole.COORDINATOR, DomainRole.HEAD), false))
                .thenReturn(List.of(coordinatorC, coordinatorD));
        when(users.findAllById(any())).thenReturn(List.of(volunteerA, volunteerB, coordinatorC, coordinatorD));
        when(programmes.findById(5L)).thenReturn(Optional.of(approvedTour(VISIT)));
    }

    // --- assigning ---------------------------------------------------------

    @Test
    void assigningWithoutTimesCoversTheWholeVisitOnTheCampusClock() {
        AssignmentView view = service.assign(5L, new AssignRequest(10L, AssignmentRole.VOLUNTEER, null, null), actor);

        assertThat(view.startsAt()).isEqualTo(at(VISIT, 9, 0));
        assertThat(view.endsAt()).isEqualTo(at(VISIT, 17, 0));
        assertThat(view.role()).isEqualTo(AssignmentRole.VOLUNTEER);
        verify(audit).save(any(AuditLog.class));
    }

    @Test
    void aPartOfTheVisitCanBeCovered() {
        AssignmentView view = service.assign(5L,
                new AssignRequest(10L, AssignmentRole.VOLUNTEER, LocalTime.of(10, 0), LocalTime.of(13, 0)), actor);

        assertThat(view.startsAt()).isEqualTo(at(VISIT, 10, 0));
        assertThat(view.endsAt()).isEqualTo(at(VISIT, 13, 0));
    }

    @Test
    void aShiftCannotStickOutOfTheVisit() {
        assertThatThrownBy(() -> service.assign(5L,
                new AssignRequest(10L, AssignmentRole.VOLUNTEER, LocalTime.of(8, 0), LocalTime.of(12, 0)), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("within the visit");
        verify(assignments, never()).saveAndFlush(any());
    }

    @Test
    void aShiftMustEndAfterItStarts() {
        assertThatThrownBy(() -> service.assign(5L,
                new AssignRequest(10L, AssignmentRole.VOLUNTEER, LocalTime.of(12, 0), LocalTime.of(12, 0)), actor))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPersonAlreadyOnAnotherTourAtThatTimeIsRefused() {
        when(assignments.existsOverlap(anyLong(), any(Instant.class), any(Instant.class))).thenReturn(true);

        assertThatThrownBy(() -> service.assign(5L, new AssignRequest(10L, AssignmentRole.VOLUNTEER, null, null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Volunteer A is already assigned");
        verify(assignments, never()).saveAndFlush(any());
    }

    @Test
    void ifTwoPeopleRaceForTheSameVolunteerTheDatabaseConstraintGivesTheSameFriendlyError() {
        when(assignments.saveAndFlush(any(TourAssignment.class)))
                .thenThrow(new DataIntegrityViolationException("no_overlapping_shifts"));

        assertThatThrownBy(() -> service.assign(5L, new AssignRequest(10L, AssignmentRole.VOLUNTEER, null, null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    void aCoordinatorIsNotAcceptedInAVolunteerSlotAndViceVersa() {
        assertThatThrownBy(() -> service.assign(5L, new AssignRequest(20L, AssignmentRole.VOLUNTEER, null, null), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volunteer");
        assertThatThrownBy(() -> service.assign(5L, new AssignRequest(10L, AssignmentRole.COORDINATOR, null, null), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coordinator");
    }

    @Test
    void onlyAnApprovedTourCanHavePeopleAssigned() {
        Programme submitted = new Programme(VISIT);
        submitted.setArrivalTime(LocalTime.of(9, 0));
        submitted.setDepartureTime(LocalTime.of(17, 0));
        when(programmes.findById(6L)).thenReturn(Optional.of(submitted));

        assertThatThrownBy(() -> service.assign(6L, new AssignRequest(10L, AssignmentRole.VOLUNTEER, null, null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved");
    }

    @Test
    void aTourWithoutTimesCannotBeStaffed() {
        Programme noTimes = approvedTour(VISIT);
        noTimes.setArrivalTime(null);
        noTimes.setDepartureTime(null);
        when(programmes.findById(7L)).thenReturn(Optional.of(noTimes));

        assertThatThrownBy(() -> service.assign(7L, new AssignRequest(10L, AssignmentRole.VOLUNTEER, null, null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("arrival and departure");
    }

    @Test
    void aTourThatAlreadyHappenedCannotBeStaffed() {
        when(programmes.findById(8L)).thenReturn(Optional.of(approvedTour(LocalDate.now(CAMPUS).minusDays(2))));

        assertThatThrownBy(() -> service.assign(8L, new AssignRequest(10L, AssignmentRole.VOLUNTEER, null, null), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already taken place");
    }

    // --- hand-over ---------------------------------------------------------

    @Test
    void aHandoverShortensTheOldShiftAndStartsTheNewOneExactlyThere() {
        TourAssignment shift = shift(100L, 10L, AssignmentRole.VOLUNTEER, at(VISIT, 9, 0), at(VISIT, 17, 0));
        when(assignments.findById(100L)).thenReturn(Optional.of(shift));

        service.handover(100L, new HandoverRequest(11L, LocalTime.of(13, 0)), actor);

        assertThat(shift.getEndsAt()).isEqualTo(at(VISIT, 13, 0));
        ArgumentCaptor<TourAssignment> saved = ArgumentCaptor.forClass(TourAssignment.class);
        verify(assignments, org.mockito.Mockito.times(2)).saveAndFlush(saved.capture());
        TourAssignment replacement = saved.getAllValues().get(1);
        assertThat(replacement.getUserId()).isEqualTo(11L);
        assertThat(replacement.getRole()).isEqualTo(AssignmentRole.VOLUNTEER);
        assertThat(replacement.getStartsAt()).isEqualTo(at(VISIT, 13, 0));
        assertThat(replacement.getEndsAt()).isEqualTo(at(VISIT, 17, 0));
    }

    @Test
    void aCoordinatorCanHandOverToAnotherCoordinatorButNotToAVolunteer() {
        TourAssignment shift = shift(101L, 20L, AssignmentRole.COORDINATOR, at(VISIT, 9, 0), at(VISIT, 17, 0));
        when(assignments.findById(101L)).thenReturn(Optional.of(shift));

        service.handover(101L, new HandoverRequest(21L, LocalTime.of(12, 0)), actor);
        assertThat(shift.getEndsAt()).isEqualTo(at(VISIT, 12, 0));

        TourAssignment another = shift(102L, 20L, AssignmentRole.COORDINATOR, at(VISIT, 9, 0), at(VISIT, 17, 0));
        when(assignments.findById(102L)).thenReturn(Optional.of(another));
        assertThatThrownBy(() -> service.handover(102L, new HandoverRequest(10L, LocalTime.of(12, 0)), actor))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(another.getEndsAt()).isEqualTo(at(VISIT, 17, 0));
    }

    @Test
    void theHandoverTimeMustFallInsideTheShift() {
        TourAssignment shift = shift(103L, 10L, AssignmentRole.VOLUNTEER, at(VISIT, 9, 0), at(VISIT, 17, 0));
        when(assignments.findById(103L)).thenReturn(Optional.of(shift));

        for (LocalTime bad : new LocalTime[] {LocalTime.of(9, 0), LocalTime.of(17, 0), LocalTime.of(18, 0)}) {
            assertThatThrownBy(() -> service.handover(103L, new HandoverRequest(11L, bad), actor))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inside the current shift");
        }
        assertThat(shift.getEndsAt()).isEqualTo(at(VISIT, 17, 0));
    }

    @Test
    void youCannotHandOverToTheSamePerson() {
        TourAssignment shift = shift(104L, 10L, AssignmentRole.VOLUNTEER, at(VISIT, 9, 0), at(VISIT, 17, 0));
        when(assignments.findById(104L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> service.handover(104L, new HandoverRequest(10L, LocalTime.of(12, 0)), actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("someone else");
    }

    @Test
    void aReleasedShiftCannotBeHandedOver() {
        TourAssignment shift = shift(105L, 10L, AssignmentRole.VOLUNTEER, at(VISIT, 9, 0), at(VISIT, 17, 0));
        shift.release();
        when(assignments.findById(105L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> service.handover(105L, new HandoverRequest(11L, LocalTime.of(12, 0)), actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("released");
    }

    // --- release and date change ---------------------------------------------

    @Test
    void releasingAShiftMarksItReleasedAndIsAudited() {
        TourAssignment shift = shift(106L, 10L, AssignmentRole.VOLUNTEER, at(VISIT, 9, 0), at(VISIT, 17, 0));
        when(assignments.findById(106L)).thenReturn(Optional.of(shift));

        service.release(106L, actor);

        assertThat(shift.getStatus()).isEqualTo(AssignmentStatus.RELEASED);
        verify(audit).save(any(AuditLog.class));
    }

    @Test
    void whenATourDateMovesEveryoneIsTakenOffAndTheReleaseIsAudited() {
        when(assignments.releaseAllForProgramme(5L)).thenReturn(3);

        service.onTourDateChanged(new TourDateChangedEvent(5L, 1L));

        verify(assignments).releaseAllForProgramme(5L);
        verify(audit).save(any(AuditLog.class));
    }

    @Test
    void nothingIsAuditedWhenNobodyWasAssignedWhenTheDateMoved() {
        when(assignments.releaseAllForProgramme(5L)).thenReturn(0);

        service.onTourDateChanged(new TourDateChangedEvent(5L, 1L));

        verify(audit, never()).save(any(AuditLog.class));
    }

    // --- who is free -----------------------------------------------------------

    @Test
    void peopleAlreadyBusyAreLeftOutOfTheAvailableList() {
        when(assignments.findBusyUserIds(any(Instant.class), any(Instant.class))).thenReturn(java.util.Set.of(10L));

        List<AssigneeOption> free = service.available(5L, AssignmentRole.VOLUNTEER, null, null);

        assertThat(free).extracting(AssigneeOption::userId).containsExactly(11L);
    }

    // --- helpers -----------------------------------------------------------------

    private static AppUser user(Long id, String name) {
        AppUser user = new AppUser(name.toLowerCase().replace(' ', '.') + "@iith.ac.in", name);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Programme approvedTour(LocalDate date) {
        Programme programme = new Programme(date);
        programme.transitionTo(ProgrammeStatus.UNDER_REVIEW);
        programme.transitionTo(ProgrammeStatus.APPROVED);
        programme.setArrivalTime(LocalTime.of(9, 0));
        programme.setDepartureTime(LocalTime.of(17, 0));
        return programme;
    }

    private static TourAssignment shift(Long id, Long userId, AssignmentRole role, Instant from, Instant to) {
        TourAssignment shift = new TourAssignment(5L, userId, role, from, to, 1L);
        ReflectionTestUtils.setField(shift, "id", id);
        return shift;
    }

    private static Instant at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(CAMPUS).toInstant();
    }
}
