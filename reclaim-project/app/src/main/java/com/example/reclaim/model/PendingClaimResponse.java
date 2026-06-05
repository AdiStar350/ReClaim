package com.example.reclaim.model;

import com.google.gson.annotations.SerializedName;

public class PendingClaimResponse {

    @SerializedName("claimId")
    private String claimId;

    @SerializedName("itemId")
    private String itemId;

    @SerializedName("itemTitle")
    private String itemTitle;

    @SerializedName("claimantId")
    private String claimantId;

    @SerializedName("validationAnswer")
    private String validationAnswer;

    @SerializedName("status")
    private String status;

    public String getClaimId() {
        return claimId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemTitle() {
        return itemTitle;
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
}
