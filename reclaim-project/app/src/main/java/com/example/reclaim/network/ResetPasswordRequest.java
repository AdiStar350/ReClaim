package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Body for {@code POST /api/auth/forgot-password/reset}.
 */
public class ResetPasswordRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("resetToken")
    private final String resetToken;

    @SerializedName("newPassword")
    private final String newPassword;

    public ResetPasswordRequest(String email, String resetToken, String newPassword) {
        this.email = email;
        this.resetToken = resetToken;
        this.newPassword = newPassword;
    }
}
