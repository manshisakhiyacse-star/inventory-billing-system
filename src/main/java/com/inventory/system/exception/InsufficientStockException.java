package com.inventory.system.exception;

public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final String productSku;
    private final int requested;
    private final int available;

    public InsufficientStockException(Long productId, String productSku, int requested, int available) {
        super(String.format("Insufficient stock for product '%s' (ID: %d). Requested: %d, Available: %d",
                productSku, productId, requested, available));
        this.productId = productId;
        this.productSku = productSku;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductSku() {
        return productSku;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
