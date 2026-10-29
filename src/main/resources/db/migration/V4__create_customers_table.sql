-- V4: Create customers table
-- Phone uniqueness is NOT enforced (documented business decision).
CREATE TABLE customers (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(150) NOT NULL,
    phone            VARCHAR(20),
    email            VARCHAR(100),
    billing_address  TEXT,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_customers_email (email),
    INDEX idx_customers_phone (phone),
    INDEX idx_customers_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
