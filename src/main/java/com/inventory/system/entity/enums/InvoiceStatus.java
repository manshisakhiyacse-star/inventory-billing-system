package com.inventory.system.entity.enums;

/**
 * Lifecycle status of an invoice.
 * CREATED: invoice generated, not yet paid.
 * PAID: payment recorded (simulated, no real gateway).
 * CANCELLED: invoice cancelled, stock may be restored.
 * VOIDED: administrative void, similar to cancelled.
 */
public enum InvoiceStatus {
    CREATED,
    PAID,
    CANCELLED,
    VOIDED
}
