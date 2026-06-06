package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for reviewing (approving or rejecting) an ownership claim
 * ({@code PUT /api/claims/{claimId}/review}).
 * <p>
 * Only the item's original owner can review a claim. The status value
 * must be either {@code "APPROVED"} or {@code "REJECTED"}.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimReviewRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "APPROVED|REJECTED",
            message = "Status must be either APPROVED or REJECTED")
    private String status;

}
