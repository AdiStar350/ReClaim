package com.example.reclaimbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing an ownership claim on an item.
 * <p>
 * Stored in the {@code claims} collection. A claim is created when a
 * user identifies an item as theirs and provides a verification answer.
 * The item's reporter then reviews and approves or rejects the claim.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "claims")
public class Claim {

    @Id
    private String id;

    /** ID of the item being claimed. */
    private String itemId;

    /** ID of the user making the claim. */
    private String claimantId;

    /** Answer to the verification question set by the reporter. */
    private String validationAnswer;

    /** Claim status: "Pending", "Approved", or "Rejected". */
    private String status;

}
