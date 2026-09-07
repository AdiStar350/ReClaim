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
 * Signup details held until the email code is checked.
 * Then a {@link User} is created and this record is deleted.
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

    /** Hashed password. Copied onto the user later. */
    private String passwordHash;

    private String name;

    private String phoneNumber;

    /** Profile picture URL. May be empty. */
    private String profilePictureUrl;

    /** Hashed 6-digit code. */
    private String otpHash;

    /** How many wrong codes have been tried. */
    private int attempts;

    private Instant createdAt;

    private Instant expiresAt;
}
