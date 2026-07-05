package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for account deletion — requires name and password confirmation.
 */
@Getter
@Setter
public class DeleteAccountRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String password;
}
