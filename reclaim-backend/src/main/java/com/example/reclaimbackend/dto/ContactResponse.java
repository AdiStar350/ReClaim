package com.example.reclaimbackend.dto;

/**
 * Contact details exposed only after a claim is approved.
 */
public class ContactResponse {

    private String name;
    private String email;
    private String phoneNumber;

    public ContactResponse() {
    }

    public ContactResponse(String name, String email, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
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
