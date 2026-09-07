package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Body for {@code POST /api/auth/register}.
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
     * Sign-up fields.
     *
     * @param email       email
     * @param password    password
     * @param name        name
     * @param phoneNumber phone
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
