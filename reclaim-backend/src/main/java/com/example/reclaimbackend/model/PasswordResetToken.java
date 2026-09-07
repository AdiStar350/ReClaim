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
 * A short-lived forgot-password record.
 * After the code is checked, {@link #resetToken} is used to set the new password.
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

    /** Hashed 6-digit code. */
    private String otpHash;

    /** How many wrong codes have been tried. */
    private int attempts;

    /** True after the code was accepted. */
    private boolean verified;

    /** Random token given after the code is accepted. May be empty. */
    private String resetToken;

    private Instant createdAt;

    private Instant expiresAt;
}
