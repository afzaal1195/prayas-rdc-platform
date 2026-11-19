package com.prayas.platform.assignment;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One person covering one stretch of one tour (a "shift"). A volunteer who
 * stays the whole day has a single shift; a hand-over splits it in two. The
 * database refuses two ACTIVE shifts for the same person that overlap in time
 * (constraint no_overlapping_shifts in V3), so a person can never be double-booked.
 */
@Entity
@Table(name = "tour_assignment")
public class TourAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "programme_id", nullable = false)
    private Long programmeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentRole role;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected TourAssignment() {
        // JPA
    }

    public TourAssignment(Long programmeId, Long userId, AssignmentRole role,
                          Instant startsAt, Instant endsAt, Long assignedBy) {
        this.programmeId = programmeId;
        this.userId = userId;
        this.role = role;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.assignedBy = assignedBy;
    }

    /** Ends this shift early (a hand-over happens at {@code when}). */
    public void endAt(Instant when) {
        this.endsAt = when;
    }

    public void release() {
        this.status = AssignmentStatus.RELEASED;
    }

    public Long getId() { return id; }
    public Long getProgrammeId() { return programmeId; }
    public Long getUserId() { return userId; }
    public AssignmentRole getRole() { return role; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public AssignmentStatus getStatus() { return status; }
}
