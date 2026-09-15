```java
public ClaimReviewRequest(String status) {
    this.status = status;
}
```

What that function does: I store the new claim status (approve or reject).
What the function is responsible for: I am the JSON body for PUT /api/claims/{claimId}/review.
Where it works: I am built in PendingClaimsActivity and PendingClaimsSheet, then passed to ReClaimApiService.reviewClaim.
Why I chose to write the code of the function the way its written: I only send status. The claim id is in the URL path.
More: I use Gson @SerializedName("status") on a final field.
