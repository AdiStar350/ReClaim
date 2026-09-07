package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Reply from {@code POST /api/auth/forgot-password/verify-otp}.
 * Holds the token used to set a new password.
 */
public class ResetTokenResponse {

    @SerializedName("resetToken")
    private String resetToken;

    public String getResetToken() {
        return resetToken;
    }
}
