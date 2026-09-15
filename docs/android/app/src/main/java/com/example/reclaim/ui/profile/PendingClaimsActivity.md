```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityPendingClaimsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    apiService = RetrofitClient.getApiService();

    setSupportActionBar(binding.toolbarPendingClaims);
    if (getSupportActionBar() != null) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    binding.toolbarPendingClaims.setNavigationOnClickListener(v -> finish());

    adapter = new PendingClaimAdapter(new PendingClaimAdapter.ReviewListener() {
        @Override
        public void onApprove(PendingClaimResponse claim) {
            reviewClaim(claim.getClaimId(), "APPROVED");
        }

        @Override
        public void onReject(PendingClaimResponse claim) {
            reviewClaim(claim.getClaimId(), "REJECTED");
        }
    });

    binding.recyclerPendingClaims.setLayoutManager(new LinearLayoutManager(this));
    binding.recyclerPendingClaims.setAdapter(adapter);

    AppDataRefresh.observeNew(this, this::loadPendingClaims);
}
```

What that function does: I show the pending-claim list and wire approve/reject.
What the function is responsible for: I am the standalone pending-claims screen.
Where it works: I inflate activity_pending_claims.xml. The adapter uses item_pending_claim.xml.
Why I chose to write the code of the function the way its written: Back finishes me. AppDataRefresh reloads after a write elsewhere.
More: I use PendingClaimAdapter and LinearLayoutManager.

```java
public void onApprove(PendingClaimResponse claim) {
    reviewClaim(claim.getClaimId(), "APPROVED");
}
```

What that function does: I send APPROVED for that claim.
What the function is responsible for: I am the adapter approve callback.
Where it works: PendingClaimAdapter.bind calls me.
Why I chose to write the code of the function the way its written: This screen does not confirm first. The sheet version does.
More: I call reviewClaim.

```java
public void onReject(PendingClaimResponse claim) {
    reviewClaim(claim.getClaimId(), "REJECTED");
}
```

What that function does: I send REJECTED for that claim.
What the function is responsible for: I am the adapter reject callback.
Where it works: PendingClaimAdapter.bind calls me.
Why I chose to write the code of the function the way its written: Same path as approve, different status string.
More: I call reviewClaim.

```java
@Override
protected void onResume() {
    super.onResume();
    loadPendingClaims();
}
```

What that function does: I reload the list whenever this screen comes back.
What the function is responsible for: I keep the list fresh after leaving and returning.
Where it works: Android calls me. I also observe AppDataRefresh.
Why I chose to write the code of the function the way its written: onResume covers cases the live signal misses.
More: I call loadPendingClaims.

```java
private void loadPendingClaims() {
    String authHeader = TokenManager.getAuthHeader(this);
    if (authHeader == null) {
        finish();
        return;
    }

    apiService.getPendingClaimsOnMyItems(authHeader).enqueue(new Callback<List<PendingClaimResponse>>() {
        @Override
        public void onResponse(@NonNull Call<List<PendingClaimResponse>> call,
                               @NonNull Response<List<PendingClaimResponse>> response) {
            if (response.isSuccessful() && response.body() != null) {
                adapter.updateClaims(response.body());
            } else {
                Toast.makeText(PendingClaimsActivity.this,
                        R.string.msg_load_pending_claims_failed, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(@NonNull Call<List<PendingClaimResponse>> call,
                              @NonNull Throwable t) {
            ConnectionToasts.showIfOnline(PendingClaimsActivity.this,
                    R.string.msg_load_pending_claims_failed, Toast.LENGTH_SHORT);
        }
    });
}
```

What that function does: I fetch pending claims on my items and fill the adapter.
What the function is responsible for: I am GET /api/claims/pending-on-my-items for this screen.
Where it works: I am called from onResume and AppDataRefresh.
Why I chose to write the code of the function the way its written: No token closes me because this screen is useless logged out.
More: I use apiService.getPendingClaimsOnMyItems.

```java
public void onResponse(@NonNull Call<List<PendingClaimResponse>> call,
                       @NonNull Response<List<PendingClaimResponse>> response) {
    if (response.isSuccessful() && response.body() != null) {
        adapter.updateClaims(response.body());
    } else {
        Toast.makeText(PendingClaimsActivity.this,
                R.string.msg_load_pending_claims_failed, Toast.LENGTH_SHORT).show();
    }
}
```

What that function does: I replace the list, or I toast a load failure.
What the function is responsible for: I am the Retrofit success path for the pending list.
Where it works: Retrofit calls me from loadPendingClaims.
Why I chose to write the code of the function the way its written: updateClaims redraws every row.
More: I use PendingClaimAdapter.updateClaims.

```java
public void onFailure(@NonNull Call<List<PendingClaimResponse>> call,
                      @NonNull Throwable t) {
    ConnectionToasts.showIfOnline(PendingClaimsActivity.this,
            R.string.msg_load_pending_claims_failed, Toast.LENGTH_SHORT);
}
```

What that function does: I toast a load failure if the API is up.
What the function is responsible for: I am the Retrofit failure path for the pending list.
Where it works: Retrofit calls me from loadPendingClaims.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline.

```java
private void reviewClaim(String claimId, String status) {
    String authHeader = TokenManager.getAuthHeader(this);
    if (authHeader == null) {
        return;
    }

    apiService.reviewClaim(authHeader, claimId, new ClaimReviewRequest(status))
            .enqueue(new Callback<Claim>() {
                @Override
                public void onResponse(@NonNull Call<Claim> call,
                                       @NonNull Response<Claim> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(PendingClaimsActivity.this,
                                R.string.msg_claim_reviewed, Toast.LENGTH_SHORT).show();
                        loadPendingClaims();
                    } else {
                        Toast.makeText(PendingClaimsActivity.this,
                                R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
                    Toast.makeText(PendingClaimsActivity.this,
                            R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
                }
            });
}
```

What that function does: I approve or reject one claim, then I reload the list.
What the function is responsible for: I am PUT /api/claims/{id}/review from this screen.
Where it works: I am called from onApprove and onReject.
Why I chose to write the code of the function the way its written: A full reload picks up sibling claims the server auto-rejects.
More: I use ClaimReviewRequest and apiService.reviewClaim.

```java
public void onResponse(@NonNull Call<Claim> call,
                       @NonNull Response<Claim> response) {
    if (response.isSuccessful()) {
        Toast.makeText(PendingClaimsActivity.this,
                R.string.msg_claim_reviewed, Toast.LENGTH_SHORT).show();
        loadPendingClaims();
    } else {
        Toast.makeText(PendingClaimsActivity.this,
                R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
    }
}
```

What that function does: I toast success and reload, or I toast a review failure.
What the function is responsible for: I am the Retrofit success path for reviewClaim.
Where it works: Retrofit calls me from reviewClaim.
Why I chose to write the code of the function the way its written: Reloading is simpler than removing one row by hand on this screen.
More: I call loadPendingClaims.

```java
public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
    Toast.makeText(PendingClaimsActivity.this,
            R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
}
```

What that function does: I toast that the review did not send.
What the function is responsible for: I am the Retrofit failure path for reviewClaim.
Where it works: Retrofit calls me from reviewClaim.
Why I chose to write the code of the function the way its written: A simple failed toast is enough here.
More: I use msg_claim_review_failed.
