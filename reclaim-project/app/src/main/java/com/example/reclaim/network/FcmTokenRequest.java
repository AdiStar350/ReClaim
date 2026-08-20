package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/** Request body for registering the device FCM token. */
public class FcmTokenRequest {

    @SerializedName("token")
    private final String token;

    public FcmTokenRequest(String token) {
        this.token = token;
    }
}
