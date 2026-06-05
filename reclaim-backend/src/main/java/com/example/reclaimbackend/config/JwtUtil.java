package com.example.reclaimbackend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility class for creating and validating JWT tokens.
 * <p>
 * Uses the HMAC-SHA256 signing algorithm with a secret key configured
 * via the {@code jwt.secret} application property.
 * </p>
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * Constructs the JWT utility with the configured secret and expiration.
     *
     * @param secret     the signing key from {@code jwt.secret}
     * @param expiration the token lifetime in milliseconds from {@code jwt.expiration}
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expiration;
    }

    /**
     * Generates a signed JWT token for the given user ID and email.
     *
     * @param userId the user's MongoDB document ID (set as the subject)
     * @param email  the user's email (stored as a custom claim)
     * @return the signed JWT string
     */
    public String generateToken(String userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts the user ID (subject) from a valid JWT token.
     *
     * @param token the JWT string
     * @return the user ID
     */
    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the email claim from a valid JWT token.
     *
     * @param token the JWT string
     * @return the email address
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    /**
     * Validates a JWT token by verifying its signature and checking
     * that it has not expired.
     *
     * @param token the JWT string
     * @return {@code true} if the token is valid and not expired
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses and verifies the JWT token, returning the claims payload.
     *
     * @param token the JWT string
     * @return the parsed {@link Claims}
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
