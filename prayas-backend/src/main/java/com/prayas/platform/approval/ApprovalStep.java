package com.prayas.platform.approval;

import jakarta.persistence.*;
import java.time.Instant;

/** One permission a tour needs from someone outside PRAYAS. See V5__approval_steps.sql. */
@Entity
@Table(name = "approval_step")
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "programme_id", nullable = false)
    private Long programmeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalKind kind;

    @Column(name = "venue_id")
    private Long venueId;

    @Column(name = "authority_contact_id")
    private Long authorityContactId;

    @Column(nullable = false)
    private int sequence = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ApprovalStep() {
        // JPA
    }

    public ApprovalStep(Long programmeId, ApprovalKind kind, Long venueId, Long authorityContactId,
                        int sequence, ApprovalStatus status) {
        this.programmeId = programmeId;
        this.kind = kind;
        this.venueId = venueId;
        this.authorityContactId = authorityContactId;
        this.sequence = sequence;
        this.status = status;
    }

    /** Someone recorded progress or an answer. */
    public void update(ApprovalStatus newStatus, String newNote, Long actorId) {
        this.status = newStatus;
        this.note = newNote;
        this.decidedBy = actorId;
        this.decidedAt = (newStatus == ApprovalStatus.APPROVED || newStatus == ApprovalStatus.DENIED)
                ? Instant.now() : null;
        this.updatedAt = Instant.now();
    }

    /** Back to the start (the date moved, or an earlier step in the chain was undone). */
    public void reset(ApprovalStatus newStatus) {
        this.status = newStatus;
        this.note = null;
        this.decidedBy = null;
        this.decidedAt = null;
        this.updatedAt = Instant.now();
    }

    public void setAuthorityContactId(Long authorityContactId) {
        this.authorityContactId = authorityContactId;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getProgrammeId() { return programmeId; }
    public ApprovalKind getKind() { return kind; }
    public Long getVenueId() { return venueId; }
    public Long getAuthorityContactId() { return authorityContactId; }
    public int getSequence() { return sequence; }
    public ApprovalStatus getStatus() { return status; }
    public String getNote() { return note; }
    public Long getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
}
