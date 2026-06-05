package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.Email;

/**
 * Request DTO for updating the authenticated user's profile.
 */
public class UpdateProfileRequest {

    private String name;

    @Email(message = "Email must be valid")
    private String email;

    private String phoneNumber;

    public UpdateProfileRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
