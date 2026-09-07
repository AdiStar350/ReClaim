package com.example.reclaim.model;

import com.google.gson.annotations.SerializedName;

/**
 * A claim on a lost or found item.
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

    @SerializedName("closureReason")
    private String closureReason;

    @SerializedName("itemTitle")
    private String itemTitle;

    @SerializedName("itemImageUrl")
    private String itemImageUrl;

    @SerializedName("itemType")
    private String itemType;

    @SerializedName("decisionAcknowledged")
    private boolean decisionAcknowledged;

    /**
     * Empty constructor for Gson.
     */
    public Claim() {
    }

    /**
     * Builds a claim with the main fields.
     *
     * @param id               claim id
     * @param itemId           item being claimed
     * @param claimantId       who is claiming
     * @param validationAnswer answer to the check question
     * @param status           e.g. Pending, Approved, Rejected
     */
    public Claim(String id, String itemId, String claimantId,
                 String validationAnswer, String status) {
        this.id = id;
        this.itemId = itemId;
        this.claimantId = claimantId;
        this.validationAnswer = validationAnswer;
        this.status = status;
    }

    // Getters

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

    public String getClosureReason() {
        return closureReason;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public String getItemImageUrl() {
        return itemImageUrl;
    }

    public String getItemType() {
        return itemType;
    }

    public boolean isDecisionAcknowledged() {
        return decisionAcknowledged;
    }

    // Setters

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

    public void setClosureReason(String closureReason) {
        this.closureReason = closureReason;
    }
}
