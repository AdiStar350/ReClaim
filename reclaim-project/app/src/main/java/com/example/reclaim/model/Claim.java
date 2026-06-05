package com.example.reclaim.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data model representing a claim on a lost or found item.
 * <p>
 * A claim is created when a user identifies an item as theirs and
 * provides a verification answer. The reporter then reviews the claim
 * and can accept or reject it.
 * </p>
 */
public class Claim {

    @SerializedName("id")
    private String id;

    @SerializedName("itemId")
    private String itemId;

    @SerializedName("claimantId")
    private String claimantId;

    @SerializedName("validationAnswer")
    private String validationAnswer;

    @SerializedName("status")
    private String status;

    /**
     * Default no-arg constructor required by Gson.
     */
    public Claim() {
    }

    /**
     * Full constructor for creating a Claim with all fields.
     *
     * @param id               unique identifier
     * @param itemId           ID of the item being claimed
     * @param claimantId       ID of the user making the claim
     * @param validationAnswer answer to the verification question
     * @param status           current status (e.g. "Pending", "Approved", "Rejected")
     */
    public Claim(String id, String itemId, String claimantId,
                 String validationAnswer, String status) {
        this.id = id;
        this.itemId = itemId;
        this.claimantId = claimantId;
        this.validationAnswer = validationAnswer;
        this.status = status;
    }

    // ── Getters ──────────────────────────────────────────────────────────

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

    // ── Setters ──────────────────────────────────────────────────────────

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
