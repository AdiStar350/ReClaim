package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code POST /api/auth/register/send-otp}.
 * <p>
 * Carries the full registration payload (including the optional Firebase
 * Storage profile picture URL). The server stores it in a pending record and
 * emails a 6-digit code; the account is only created after verification.
 * </p>
 */
public class SendOtpRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    @SerializedName("name")
    private final String name;

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    @SerializedName("profilePictureUrl")
    private final String profilePictureUrl;

    public SendOtpRequest(String email, String password, String name,
                          String phoneNumber, String profilePictureUrl) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = profilePictureUrl;
    }
}
