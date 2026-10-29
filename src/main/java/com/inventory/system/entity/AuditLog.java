package com.inventory.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * General-purpose audit log for important business actions.
 * Records: stock adjustments, invoice creation/cancellation, user admin changes.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "performed_by", nullable = false, length = 50)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
