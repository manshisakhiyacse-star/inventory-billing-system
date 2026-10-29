-- V2: Create products table
-- Stores the product catalog with pricing, stock levels, and reorder thresholds.
-- Uses DECIMAL for money to avoid floating-point rounding errors.
CREATE TABLE products (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sku                VARCHAR(50)   NOT NULL,
    name               VARCHAR(200)  NOT NULL,
    description        TEXT,
    category           VARCHAR(100),
    unit_price         DECIMAL(12,2) NOT NULL,
    cost_price         DECIMAL(12,2) NOT NULL,
    quantity_in_stock  INT           NOT NULL DEFAULT 0,
    reorder_threshold  INT           NOT NULL DEFAULT 10,
    active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version            BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_products_sku UNIQUE (sku),
    INDEX idx_products_category (category),
    INDEX idx_products_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
