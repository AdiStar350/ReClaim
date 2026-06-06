package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for updating the authenticated user's profile.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    private String name;

    @Email(message = "Email must be valid")
    private String email;

    private String phoneNumber;

}
