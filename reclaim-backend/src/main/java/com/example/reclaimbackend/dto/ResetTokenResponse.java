package com.example.reclaimbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO returned by {@code POST /api/auth/forgot-password/verify-otp}.
 * <p>
 * Carries the opaque reset token the client must present when submitting the
 * new password.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetTokenResponse {
    private String resetToken;
}
