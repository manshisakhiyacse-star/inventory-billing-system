-- V5: Create invoices table
-- tax_rate as DECIMAL(5,4): e.g., 0.1800 = 18%.
-- stock_restored prevents duplicate stock restoration on cancellation.
-- payment_method/payment_status are record-keeping only (no real payment gateway).
CREATE TABLE invoices (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    invoice_number   VARCHAR(30)   NOT NULL,
    customer_id      BIGINT        NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    subtotal         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_rate         DECIMAL(5,4)  NOT NULL DEFAULT 0.0000,
    tax_amount       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    grand_total      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    payment_method   VARCHAR(20),
    payment_status   VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    notes            VARCHAR(500),
    created_by       VARCHAR(50)   NOT NULL,
    stock_restored   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    INDEX idx_invoices_customer_id (customer_id),
    INDEX idx_invoices_status (status),
    INDEX idx_invoices_created_at (created_at),
    CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
