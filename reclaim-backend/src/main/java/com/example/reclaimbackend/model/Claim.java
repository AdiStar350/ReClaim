package com.example.reclaimbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * An ownership claim in the {@code claims} collection.
 * The reporter later approves or declines it.
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

    /** The item being claimed. */
    private String itemId;

    /** Who made the claim. */
    private String claimantId;

    /** Answer to the reporter's hidden question. */
    private String validationAnswer;

    /** "Pending", "Approved", or "Rejected". */
    private String status;

    /**
     * Why it ended. See {@link ClosureReasons}. Empty while still pending.
     */
    private String closureReason;

    /**
     * Whether the claimant has dismissed the decision card.
     */
    private boolean decisionAcknowledged;

    /** Item title. Filled on read, not saved. */
    @Transient
    private String itemTitle;

    /** Item photo URL. Filled on read, not saved. */
    @Transient
    private String itemImageUrl;

    /** "Lost" or "Found". Filled on read, not saved. */
    @Transient
    private String itemType;

}
