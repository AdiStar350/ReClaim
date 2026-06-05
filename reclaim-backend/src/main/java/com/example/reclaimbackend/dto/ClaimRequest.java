package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.NotBlank;

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
public class ClaimRequest {

    @NotBlank(message = "Item ID is required")
    private String itemId;

    @NotBlank(message = "Validation answer is required")
    private String validationAnswer;

    public ClaimRequest() {
    }

    public ClaimRequest(String itemId, String validationAnswer) {
        this.itemId = itemId;
        this.validationAnswer = validationAnswer;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getValidationAnswer() {
        return validationAnswer;
    }

    public void setValidationAnswer(String validationAnswer) {
        this.validationAnswer = validationAnswer;
    }
}
