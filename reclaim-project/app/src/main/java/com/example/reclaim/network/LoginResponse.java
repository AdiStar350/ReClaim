package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Reply from {@code POST /api/auth/login}.
 */
public class LoginResponse {

    @SerializedName("token")
    private String token;

    /**
     * Empty constructor for Gson.
     */
    public LoginResponse() {
    }

    /**
     * Reply with a token.
     *
     * @param token the token
     */
    public LoginResponse(String token) {
        this.token = token;
    }

    /**
     * The token.
     *
     * @return the token
     */
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
