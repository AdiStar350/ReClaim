package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for submitting an ownership claim
 * ({@code POST /api/claims}).
 * <p>
 * Contains the ID of the item being claimed and the claimant's
 * answer to the hidden verification question. The {@code claimantId}
 * is extracted from the JWT token on the server side.
 * </p>
 */
public class ClaimRequest {

    @SerializedName("itemId")
    private final String itemId;

    @SerializedName("validationAnswer")
    private final String validationAnswer;

    /**
     * Creates a new claim request.
     *
     * @param itemId           the ID of the item to claim
     * @param validationAnswer the answer to the verification question
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
