package com.example.reclaimbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Transient registration record held between OTP dispatch and verification.
 * <p>
 * Stored in the {@code pending_registrations} collection. A document lives
 * here only while the user is verifying their email; once the correct OTP is
 * submitted, the data is promoted into a real {@link User} document and the
 * pending record is deleted. The plaintext password and OTP are never stored —
 * only their BCrypt hashes.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "pending_registrations")
public class PendingRegistration {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    /** BCrypt hash of the chosen password (promoted verbatim to the User). */
    private String passwordHash;

    private String name;

    private String phoneNumber;

    /** Firebase Storage download URL for the profile picture (nullable). */
    private String profilePictureUrl;

    /** BCrypt hash of the 6-digit one-time code. */
    private String otpHash;

    /** Number of incorrect OTP submissions so far. */
    private int attempts;

    private Instant createdAt;

    private Instant expiresAt;
}
