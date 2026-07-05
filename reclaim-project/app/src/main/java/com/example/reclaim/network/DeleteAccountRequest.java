package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code DELETE /api/users/me}.
 */
public class DeleteAccountRequest {

    @SerializedName("name")
    private final String name;

    @SerializedName("password")
    private final String password;

    public DeleteAccountRequest(String name, String password) {
        this.name = name;
        this.password = password;
    }
}
