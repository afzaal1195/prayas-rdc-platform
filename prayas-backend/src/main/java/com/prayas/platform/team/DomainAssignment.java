package com.prayas.platform.team;

import jakarta.persistence.*;
import java.time.Instant;

/** One person on one domain's team for one tour. See V6__domain_assignments.sql. */
@Entity
@Table(name = "domain_assignment")
public class DomainAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "programme_id", nullable = false)
    private Long programmeId;

    @Column(name = "domain_id", nullable = false)
    private Long domainId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DomainAssignment() {
        // JPA
    }

    public DomainAssignment(Long programmeId, Long domainId, Long userId, TeamRole role, Long assignedBy) {
        this.programmeId = programmeId;
        this.domainId = domainId;
        this.userId = userId;
        this.role = role;
        this.assignedBy = assignedBy;
    }

    public Long getId() { return id; }
    public Long getProgrammeId() { return programmeId; }
    public Long getDomainId() { return domainId; }
    public Long getUserId() { return userId; }
    public TeamRole getRole() { return role; }
}
