package com.inventory.system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Customer entity for billing.
 * Phone is not enforced unique — multiple contacts may share a phone
 * in a small business context. This is documented as a design decision.
 */
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customers_email", columnList = "email"),
        @Index(name = "idx_customers_phone", columnList = "phone"),
        @Index(name = "idx_customers_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
