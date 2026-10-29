package com.inventory.system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A single line item on an invoice.
 * Captures an immutable snapshot of product details at the time of sale,
 * so historical invoices remain accurate even if the product is later updated.
 */
@Entity
@Table(name = "invoice_line_items", indexes = {
        @Index(name = "idx_ili_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_ili_product_id", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Snapshot: product SKU at time of sale. */
    @Column(name = "product_sku", nullable = false, length = 50)
    private String productSku;

    /** Snapshot: product name at time of sale. */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /** Snapshot: unit price at time of sale. */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /** line_total = unit_price × quantity, rounded HALF_UP. */
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}
