package com.inventory.system.entity;

import com.inventory.system.entity.enums.InvoiceStatus;
import com.inventory.system.entity.enums.PaymentMethod;
import com.inventory.system.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Invoice / bill for a customer.
 * All monetary fields use BigDecimal with DECIMAL(12,2).
 * The tax_rate is stored as DECIMAL(5,4) — e.g., 0.1800 = 18%.
 * stock_restored flag prevents duplicate stock restoration on cancellation.
 *
 * NOTE: payment_method and payment_status are record-keeping only.
 * This system does NOT integrate with a real payment gateway.
 */
@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoices_number", columnList = "invoice_number", unique = true),
        @Index(name = "idx_invoices_customer_id", columnList = "customer_id"),
        @Index(name = "idx_invoices_status", columnList = "status"),
        @Index(name = "idx_invoices_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.CREATED;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    /** Idempotency flag: true if stock was already restored after cancellation. */
    @Column(name = "stock_restored", nullable = false)
    @Builder.Default
    private Boolean stockRestored = false;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Helper to add a line item and maintain the bidirectional relationship. */
    public void addLineItem(InvoiceLineItem item) {
        lineItems.add(item);
        item.setInvoice(this);
    }
}
