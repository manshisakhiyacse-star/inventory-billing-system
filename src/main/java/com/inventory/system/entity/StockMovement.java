package com.inventory.system.entity;

import com.inventory.system.entity.enums.StockMovementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable audit record for every stock change.
 * Each row captures what changed, by how much, the resulting quantity,
 * the reason, and who performed it.
 */
@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_sm_product_id", columnList = "product_id"),
        @Index(name = "idx_sm_movement_type", columnList = "movement_type"),
        @Index(name = "idx_sm_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private StockMovementType movementType;

    /** Positive = stock in, negative = stock out. */
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    /** Snapshot of product stock after this movement. */
    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(length = 500)
    private String reason;

    /** Optional reference (e.g., invoice ID for SALE movements). */
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "performed_by", nullable = false, length = 50)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
