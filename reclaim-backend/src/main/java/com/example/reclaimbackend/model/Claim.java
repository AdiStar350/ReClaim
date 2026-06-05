package com.example.reclaimbackend.model;

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

    /** Default constructor required by Spring Data. */
    public Claim() {
    }

    public Claim(String itemId, String claimantId,
                 String validationAnswer, String status) {
        this.itemId = itemId;
        this.claimantId = claimantId;
        this.validationAnswer = validationAnswer;
        this.status = status;
    }

    // ── Getters ──────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getItemId() {
        return itemId;
    }

    public String getClaimantId() {
        return claimantId;
    }

    public String getValidationAnswer() {
        return validationAnswer;
    }

    public String getStatus() {
        return status;
    }

    // ── Setters ──────────────────────────────────────────────────────

    public void setId(String id) {
        this.id = id;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setClaimantId(String claimantId) {
        this.claimantId = claimantId;
    }

    public void setValidationAnswer(String validationAnswer) {
        this.validationAnswer = validationAnswer;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
