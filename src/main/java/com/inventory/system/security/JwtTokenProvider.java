package com.inventory.system.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Handles JWT token generation, parsing, and validation using jjwt 0.12.x.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationMs;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs,
            @Value("${app.jwt.issuer:inventory-billing-system}") String issuer) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Ensure secret meets HMAC-SHA256 minimum length requirement (32 bytes / 256 bits)
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes (256 bits) long.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    /**
     * Generate JWT from authenticated UserDetails.
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return buildToken(userDetails.getUsername(), roles);
    }

    /**
     * Generate JWT directly with username and role.
     */
    public String generateToken(String username, String role) {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return buildToken(username, authority);
    }

    private String buildToken(String username, String roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extract username subject from valid JWT.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Validate JWT signature, expiration, and formatting.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token format: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty or invalid: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT token validation failed: {}", ex.getMessage());
        }
        return false;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
