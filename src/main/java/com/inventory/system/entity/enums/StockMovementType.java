package com.inventory.system.entity.enums;

/**
 * Types of stock movements for audit trail.
 * PURCHASE: stock added from supplier.
 * SALE: stock deducted by invoice.
 * ADJUSTMENT: manual stock correction with reason.
 * CANCELLATION_REVERSAL: stock restored when invoice is cancelled.
 */
public enum StockMovementType {
    PURCHASE,
    SALE,
    ADJUSTMENT,
    CANCELLATION_REVERSAL
}
