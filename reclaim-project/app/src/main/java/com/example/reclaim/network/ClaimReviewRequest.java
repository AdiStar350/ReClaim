package com.example.reclaim.network;

import com.google.gson.annotations.SerializedName;

public class ClaimReviewRequest {

    @SerializedName("status")
    private final String status;

    public ClaimReviewRequest(String status) {
        this.status = status;
    }
}
