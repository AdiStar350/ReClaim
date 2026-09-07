package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Body for {@code POST /api/auth/register/send-otp}.
 * Sign-up fields; the account is made after the code is checked.
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
