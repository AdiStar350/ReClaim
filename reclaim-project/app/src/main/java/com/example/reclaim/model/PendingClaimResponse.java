package com.example.reclaim.model;

import com.google.gson.annotations.SerializedName;

public class PendingClaimResponse {

    @SerializedName("claimId")
    private String claimId;

    @SerializedName("itemId")
    private String itemId;

    @SerializedName("itemTitle")
    private String itemTitle;

    @SerializedName("itemImageUrl")
    private String itemImageUrl;

    @SerializedName("claimantId")
    private String claimantId;

    @SerializedName("claimantName")
    private String claimantName;

    @SerializedName("validationAnswer")
    private String validationAnswer;

    @SerializedName("status")
    private String status;

    @SerializedName("verificationQuestion")
    private String verificationQuestion;

    public String getClaimId() {
        return claimId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public String getItemImageUrl() {
        return itemImageUrl;
    }

    public String getClaimantId() {
        return claimantId;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public String getValidationAnswer() {
        return validationAnswer;
    }

    public String getStatus() {
        return status;
    }

    public String getVerificationQuestion() {
        return verificationQuestion;
    }
}
