package com.prayas.platform.requirement;

import com.prayas.platform.domain.Domain;
import com.prayas.platform.tour.Programme;
import com.prayas.platform.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A task a domain owes a programme, e.g. "arrange lunch" or "find volunteers".
 * Spawned automatically when a tour is approved (see TourDecisionService).
 */
@Entity
@Table(name = "requirement",
       uniqueConstraints = @UniqueConstraint(columnNames = {"programme_id", "kind"}))
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementStatus status = RequirementStatus.PENDING;

    // Set when something changes after the domain has already acted
    // (e.g. the school revises headcount after lunch was arranged).
    @Column(name = "needs_attention", nullable = false)
    private boolean needsAttention = false;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private AppUser updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Requirement() {
        // JPA
    }

    public Requirement(Programme programme, Domain domain, RequirementKind kind, LocalDate dueDate) {
        this.programme = programme;
        this.domain = domain;
        this.kind = kind;
        this.dueDate = dueDate;
    }

    public void markNeedsAttention() {
        this.needsAttention = true;
        this.updatedAt = Instant.now();
    }

    public void updateStatus(RequirementStatus status, AppUser actor) {
        this.status = status;
        this.needsAttention = false;
        this.updatedBy = actor;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProgrammeId() {
        return programme.getId();
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public RequirementKind getKind() {
        return kind;
    }

    public Domain getDomain() {
        return domain;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public boolean needsAttention() {
        return needsAttention;
    }
}
