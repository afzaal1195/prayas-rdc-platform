package com.prayas.platform.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId; // null for public/school actions

    @Column(name = "actor_label")
    private String actorLabel; // e.g. "school-via-token"

    @Column(nullable = false)
    private String action; // e.g. TOUR_APPROVED

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {
        // JPA
    }

    public static AuditLog byUser(Long actorUserId, String action, String entityType,
                                   Long entityId, Map<String, Object> details) {
        AuditLog log = new AuditLog();
        log.actorUserId = actorUserId;
        log.action = action;
        log.entityType = entityType;
        log.entityId = entityId;
        log.details = details;
        return log;
    }
}
