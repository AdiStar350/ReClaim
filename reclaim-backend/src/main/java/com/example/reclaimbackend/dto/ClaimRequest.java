package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body for a new claim. The claimant comes from the token, not here.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClaimRequest {

    @NotBlank(message = "Item ID is required")
    private String itemId;

    @NotBlank(message = "Validation answer is required")
    private String validationAnswer;

}
