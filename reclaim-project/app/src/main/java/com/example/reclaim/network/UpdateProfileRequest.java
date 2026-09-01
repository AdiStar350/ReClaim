package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequest {

    @SerializedName("name")
    private final String name;

    @SerializedName("email")
    private final String email;

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    @SerializedName("profilePictureUrl")
    private final String profilePictureUrl;

    public UpdateProfileRequest(String name, String email, String phoneNumber) {
        this(name, email, phoneNumber, null);
    }

    public UpdateProfileRequest(String name, String email, String phoneNumber,
                                String profilePictureUrl) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = profilePictureUrl;
    }
}
