package com.example.reclaimbackend.dto;

/**
 * Pending claim enriched with item title for owner review screens.
 */
public class PendingClaimResponse {

    private String claimId;
    private String itemId;
    private String itemTitle;
    private String claimantId;
    private String validationAnswer;
    private String status;

    public PendingClaimResponse() {
    }

    public PendingClaimResponse(String claimId, String itemId, String itemTitle,
                                String claimantId, String validationAnswer, String status) {
        this.claimId = claimId;
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.claimantId = claimantId;
        this.validationAnswer = validationAnswer;
        this.status = status;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public String getClaimantId() {
        return claimantId;
    }

    public void setClaimantId(String claimantId) {
        this.claimantId = claimantId;
    }

    public String getValidationAnswer() {
        return validationAnswer;
    }

    public void setValidationAnswer(String validationAnswer) {
        this.validationAnswer = validationAnswer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
