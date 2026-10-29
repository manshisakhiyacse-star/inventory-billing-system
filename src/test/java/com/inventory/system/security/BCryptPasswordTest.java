package com.inventory.system.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptPasswordTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Test
    @DisplayName("Verify seed admin password matches verified BCrypt hash in V8 migration")
    void testSeedAdminPasswordHashMatches() {
        String seedHash = "$2a$12$Q5sjMrIT3v1MDvHr4PtrtuhnCf9ylTuwfelXw/.LEY53sw0gHFlvq";
        assertTrue(encoder.matches("admin123", seedHash), "Seed hash must match 'admin123'");
    }
}
