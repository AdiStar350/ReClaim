package com.example.reclaimbackend.dto;

import com.example.reclaimbackend.model.ClosureReasons;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for an owner-initiated report closure
 * ({@code PUT /api/items/{id}/close}).
 * <p>
 * Only the report's owner may close it. The closure reason must be one of
 * the owner-initiated outcomes: {@code "owner found"} or
 * {@code "owner not found"}. Claim-driven closures ("claim approved" /
 * "claim declined") are set automatically by the claim review flow.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloseReportRequest {

    @NotBlank(message = "Closure reason is required")
    @Pattern(regexp = ClosureReasons.OWNER_INITIATED_PATTERN,
            message = "Closure reason must be 'owner found' or 'owner not found'")
    private String closureReason;

}
