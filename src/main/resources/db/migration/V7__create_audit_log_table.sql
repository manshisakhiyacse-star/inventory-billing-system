-- V7: Create audit_log table
-- General-purpose audit trail for business-critical actions.
CREATE TABLE audit_log (
    id            BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    action        VARCHAR(50) NOT NULL,
    entity_type   VARCHAR(50) NOT NULL,
    entity_id     BIGINT,
    details       TEXT,
    performed_by  VARCHAR(50) NOT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
