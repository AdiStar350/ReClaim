package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code POST /api/auth/forgot-password/send-otp}.
 */
public class ForgotPasswordRequest {

    @SerializedName("email")
    private final String email;

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }
}
