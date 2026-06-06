package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for submitting a new ownership claim
 * ({@code POST /api/claims}).
 * <p>
 * Contains the ID of the item being claimed and the claimant's
 * answer to the hidden verification question. The {@code claimantId}
 * is not included here — it is extracted from the JWT token by the
 * controller.
 * </p>
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
