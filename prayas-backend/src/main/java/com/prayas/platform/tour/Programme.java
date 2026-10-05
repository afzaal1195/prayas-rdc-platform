package com.prayas.platform.tour;

import com.prayas.platform.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "programme")
public class Programme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "programme_type", nullable = false)
    private ProgrammeType programmeType = ProgrammeType.CAMPUS_TOUR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgrammeStatus status = ProgrammeStatus.SUBMITTED;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "proposed_date")
    private LocalDate proposedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private AppUser decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", columnDefinition = "text")
    private String decisionNote;

    @Version
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Programme() {
        // JPA
    }

    public Programme(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    /** Moves status, throwing if the transition isn't allowed. Caller sets any related fields. */
    public void transitionTo(ProgrammeStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException(
                    "Cannot move programme %d from %s to %s".formatted(id, status, next));
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ProgrammeStatus getStatus() {
        return status;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDate getProposedDate() {
        return proposedDate;
    }

    public void setProposedDate(LocalDate proposedDate) {
        this.proposedDate = proposedDate;
    }

    public void setDecidedBy(AppUser decidedBy) {
        this.decidedBy = decidedBy;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

        public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    /**
     * The school (not staff) asking to push their own request to a later
     * date, before any decision has been made -- distinct from
     * RESCHEDULE_PROPOSED, which is staff proposing a date to the school.
     * Status is left as-is (still SUBMITTED/UNDER_REVIEW); only the date
     * moves. Caller enforces the "not yet decided" and "later than the
     * current date" rules.
     */
    public void changeVisitDate(LocalDate newVisitDate) {
        this.visitDate = newVisitDate;
        this.updatedAt = Instant.now();
    }

    public int getVersion() {
        return version;
    }
}
