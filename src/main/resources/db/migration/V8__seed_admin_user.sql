-- V8: Seed default admin user
-- Password: 'admin123' hashed with BCrypt (strength 12).
-- This is a LOCAL DEVELOPMENT default only.
-- In production, inject ADMIN_DEFAULT_PASSWORD via environment variable
-- and change this password immediately after first login.
--
-- BCrypt hash for 'admin123':
-- Generated with: new BCryptPasswordEncoder(12).encode("admin123")
INSERT INTO users (username, email, password_hash, role, active, created_at, updated_at)
VALUES (
    'admin',
    'admin@inventory.local',
    '$2a$12$Q5sjMrIT3v1MDvHr4PtrtuhnCf9ylTuwfelXw/.LEY53sw0gHFlvq',
    'ADMIN',
    TRUE,
    NOW(6),
    NOW(6)
);
