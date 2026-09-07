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
 * Body for the owner closing a report.
 * Reason is {@code "owner found"} or {@code "owner not found"}.
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
