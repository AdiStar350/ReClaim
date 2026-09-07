package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Body for {@code POST /api/auth/register/verify-otp}.
 */
public class VerifyOtpRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("code")
    private final String code;

    public VerifyOtpRequest(String email, String code) {
        this.email = email;
        this.code = code;
    }
}
