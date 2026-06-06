package com.example.reclaimbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pending claim enriched with item title for owner review screens.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingClaimResponse {

    private String claimId;
    private String itemId;
    private String itemTitle;
    private String claimantId;
    private String validationAnswer;
    private String status;

}
