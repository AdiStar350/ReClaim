package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Name and password needed to delete an account.
 */
@Getter
@Setter
public class DeleteAccountRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String password;
}
