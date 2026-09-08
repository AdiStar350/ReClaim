package com.example.reclaimbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A pending claim plus the item title, for the owner's review list.
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
    private String itemImageUrl;
    private String claimantId;
    private String claimantName;
    private String validationAnswer;
    private String status;

    /** The owner's hidden question for this item. */
    private String verificationQuestion;

}
