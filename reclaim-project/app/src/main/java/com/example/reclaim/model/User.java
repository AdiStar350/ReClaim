package com.example.reclaim.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data model representing a user of the ReClaim application.
 * <p>
 * Fields are annotated with {@link SerializedName} for Gson/Retrofit
 * JSON serialization and deserialization.
 * </p>
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

    /**
     * Default no-arg constructor required by Gson.
     */
    public User() {
    }

    /**
     * Full constructor for creating a User with all fields.
     *
     * @param id          unique identifier
     * @param email       user's email address
     * @param name        user's full name
     * @param phoneNumber user's phone number
     */
    public User(String id, String email, String name, String phoneNumber) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    // ── Getters ──────────────────────────────────────────────────────────

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

    // ── Setters ──────────────────────────────────────────────────────────

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
}
