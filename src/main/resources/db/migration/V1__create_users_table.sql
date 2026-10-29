-- V1: Create users table
-- Stores application users with BCrypt password hashes and role-based access.
CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
