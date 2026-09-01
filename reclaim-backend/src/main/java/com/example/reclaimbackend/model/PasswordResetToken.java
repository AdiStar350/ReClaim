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
 * Short-lived record backing the "forgot password" flow.
 * <p>
 * Stored in the {@code password_reset_otps} collection. A document is created
 * when a user requests a reset code and is deleted once the password is
 * changed (or when it expires / is replaced). Only the BCrypt hash of the OTP
 * is stored. After the OTP is verified, {@link #verified} is set and a random
 * {@link #resetToken} is issued that must be presented to actually change the
 * password — this decouples "prove you own the email" from "set new password".
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "password_reset_otps")
public class PasswordResetToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    /** BCrypt hash of the 6-digit one-time code. */
    private String otpHash;

    /** Number of incorrect OTP submissions so far. */
    private int attempts;

    /** Set once the OTP has been successfully verified. */
    private boolean verified;

    /** Random opaque token issued on successful verification (nullable). */
    private String resetToken;

    private Instant createdAt;

    private Instant expiresAt;
}
