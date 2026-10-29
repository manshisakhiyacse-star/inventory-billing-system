-- V6: Create invoice_line_items table
-- Snapshots product_sku, product_name, unit_price so historical invoices
-- remain accurate even if the product is later updated.
CREATE TABLE invoice_line_items (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    invoice_id    BIGINT        NOT NULL,
    product_id    BIGINT        NOT NULL,
    product_sku   VARCHAR(50)   NOT NULL,
    product_name  VARCHAR(200)  NOT NULL,
    unit_price    DECIMAL(12,2) NOT NULL,
    quantity      INT           NOT NULL,
    line_total    DECIMAL(12,2) NOT NULL,
    INDEX idx_ili_invoice_id (invoice_id),
    INDEX idx_ili_product_id (product_id),
    CONSTRAINT fk_ili_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_ili_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
