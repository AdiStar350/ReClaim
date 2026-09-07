package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Pattern;

/**
 * Approve or decline a claim. Status is {@code APPROVED} or {@code REJECTED}.
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
