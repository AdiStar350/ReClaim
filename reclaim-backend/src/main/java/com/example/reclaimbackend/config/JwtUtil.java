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
 * Makes and checks tokens. Signed with {@code jwt.secret}.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * Loads the signing key and how long tokens last.
     *
     * @param secret     from {@code jwt.secret}
     * @param expiration lifetime in milliseconds from {@code jwt.expiration}
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expiration;
    }

    /**
     * Makes a signed token for this user.
     *
     * @param userId the user id (subject)
     * @param email  the user's email
     * @return the token
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
     * Reads the user id from a token.
     *
     * @param token the token
     * @return the user id
     */
    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Reads the email from a token.
     *
     * @param token the token
     * @return the email
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    /**
     * Checks the signature and that the token has not expired.
     *
     * @param token the token
     * @return {@code true} if it is still good
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
     * Reads the token body after checking the signature.
     *
     * @param token the token
     * @return the claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
