package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code POST /api/auth/register/resend-otp}.
 */
public class ResendOtpRequest {

    @SerializedName("email")
    private final String email;

    public ResendOtpRequest(String email) {
        this.email = email;
    }
}
