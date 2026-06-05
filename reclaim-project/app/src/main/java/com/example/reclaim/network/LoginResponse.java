package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Response body from the login endpoint ({@code POST /api/auth/login}).
 * <p>
 * Contains the JWT access token issued upon successful authentication.
 * </p>
 */
public class LoginResponse {

    @SerializedName("token")
    private String token;

    /**
     * Default no-arg constructor required by Gson.
     */
    public LoginResponse() {
    }

    /**
     * Creates a response with the given token.
     *
     * @param token the JWT access token
     */
    public LoginResponse(String token) {
        this.token = token;
    }

    /**
     * Returns the JWT access token.
     *
     * @return the token string
     */
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
