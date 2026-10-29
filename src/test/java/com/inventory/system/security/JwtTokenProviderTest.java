package com.inventory.system.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS, "inventory-billing-system");
    }

    @Test
    @DisplayName("Should generate valid JWT token and extract subject")
    void testGenerateAndValidateToken() {
        String token = jwtTokenProvider.generateToken("admin", "ADMIN");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("admin", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("Should reject invalid or tampered JWT token")
    void testRejectInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Should reject token when signed with different secret key")
    void testRejectTokenFromDifferentKey() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "DifferentSecretKeyMustBe32BytesOrLongerForHmacSha256!",
                EXPIRATION_MS,
                "inventory-billing-system"
        );

        String token = otherProvider.generateToken("admin", "ADMIN");
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testRejectExpiredToken() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                SECRET,
                -1000, // Expired in the past
                "inventory-billing-system"
        );

        String token = shortLivedProvider.generateToken("admin", "ADMIN");
        assertFalse(jwtTokenProvider.validateToken(token));
    }
}
