package com.example.reclaim.model;

import com.google.gson.annotations.SerializedName;

/**
 * A ReClaim user.
 */
public class User {

    @SerializedName("id")
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("name")
    private String name;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("profilePictureUrl")
    private String profilePictureUrl;

    /**
     * Empty constructor for Gson.
     */
    public User() {
    }

    /**
     * Builds a user with the main fields.
     *
     * @param id          user id
     * @param email       email
     * @param name        name
     * @param phoneNumber phone
     */
    public User(String id, String email, String name, String phoneNumber) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    // Setters

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
