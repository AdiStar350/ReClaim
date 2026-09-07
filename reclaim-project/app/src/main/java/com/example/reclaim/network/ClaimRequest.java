package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Body for {@code POST /api/claims}.
 * Item id and the answer to the check question.
 */
public class ClaimRequest {

    @SerializedName("itemId")
    private final String itemId;

    @SerializedName("validationAnswer")
    private final String validationAnswer;

    /**
     * Item id and answer.
     *
     * @param itemId           item to claim
     * @param validationAnswer answer to the check question
     */
    public ClaimRequest(String itemId, String validationAnswer) {
        this.itemId = itemId;
        this.validationAnswer = validationAnswer;
    }

    public String getItemId() {
        return itemId;
    }

    public String getValidationAnswer() {
        return validationAnswer;
    }
}
