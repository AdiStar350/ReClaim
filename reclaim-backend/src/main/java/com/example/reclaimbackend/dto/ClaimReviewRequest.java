package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for reviewing (approving or rejecting) an ownership claim
 * ({@code PUT /api/claims/{claimId}/review}).
 * <p>
 * Only the item's original owner can review a claim. The status value
 * must be either {@code "APPROVED"} or {@code "REJECTED"}.
 * </p>
 */
public class ClaimReviewRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "APPROVED|REJECTED",
            message = "Status must be either APPROVED or REJECTED")
    private String status;

    public ClaimReviewRequest() {
    }

    public ClaimReviewRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
