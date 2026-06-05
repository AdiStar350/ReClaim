package com.example.reclaimbackend.dto;

/**
 * Response DTO for the {@code POST /api/auth/login} endpoint.
 * <p>
 * Contains the JWT access token issued upon successful authentication.
 * </p>
 */
public class LoginResponse {

    private String token;

    public LoginResponse() {
    }

    public LoginResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
