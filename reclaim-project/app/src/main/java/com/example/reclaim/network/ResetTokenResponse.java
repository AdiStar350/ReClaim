package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Response body from {@code POST /api/auth/forgot-password/verify-otp}.
 * <p>
 * Carries the opaque reset token needed to submit a new password.
 * </p>
 */
public class ResetTokenResponse {

    @SerializedName("resetToken")
    private String resetToken;

    public String getResetToken() {
        return resetToken;
    }
}
