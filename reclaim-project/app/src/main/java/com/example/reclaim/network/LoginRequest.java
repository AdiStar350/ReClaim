package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Body for {@code POST /api/auth/login}.
 */
public class LoginRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    /**
     * Email and password.
     *
     * @param email    email
     * @param password password
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
