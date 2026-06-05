package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for the login endpoint ({@code POST /api/auth/login}).
 * <p>
 * Contains the user's email and password credentials.
 * </p>
 */
public class LoginRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    /**
     * Creates a new login request.
     *
     * @param email    the user's email address
     * @param password the user's password
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
