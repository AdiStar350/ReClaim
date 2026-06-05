package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for the registration endpoint
 * ({@code POST /api/auth/register}).
 * <p>
 * Contains the new user's email, password, full name, and phone number.
 * </p>
 */
public class RegisterRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    @SerializedName("name")
    private final String name;

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    /**
     * Creates a new registration request.
     *
     * @param email       the user's email address
     * @param password    the user's chosen password
     * @param name        the user's full name
     * @param phoneNumber the user's phone number
     */
    public RegisterRequest(String email, String password,
                           String name, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
