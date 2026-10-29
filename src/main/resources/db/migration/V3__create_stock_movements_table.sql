-- V3: Create stock_movements table
-- Immutable audit trail for every stock change (purchase, sale, adjustment, reversal).
CREATE TABLE stock_movements (
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT      NOT NULL,
    movement_type   VARCHAR(30) NOT NULL,
    quantity_change  INT        NOT NULL,
    quantity_after   INT        NOT NULL,
    reason          VARCHAR(500),
    reference_id    BIGINT,
    performed_by    VARCHAR(50) NOT NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_sm_product_id (product_id),
    INDEX idx_sm_movement_type (movement_type),
    INDEX idx_sm_created_at (created_at),
    CONSTRAINT fk_sm_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
