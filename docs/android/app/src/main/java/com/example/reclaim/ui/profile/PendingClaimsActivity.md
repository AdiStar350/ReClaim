# PendingClaimsActivity.java

## Overview
This is a full-screen **Activity** that lists pending claims on the signed-in user’s reports and lets the owner Approve or Reject each one via the backend (see `docs/GLOSSARY.md`). It uses `PendingClaimAdapter` and reloads when the screen resumes or when `AppDataRefresh` signals new data.

## Concepts used here
- **Activity** — full screen with toolbar and RecyclerView. See `docs/GLOSSARY.md`.
- **RecyclerView / Adapter** — `PendingClaimAdapter` shows claim cards. See `docs/GLOSSARY.md`.
- **Retrofit Call / enqueue / Callback** — loads claims and posts reviews. See `docs/GLOSSARY.md`.
- **JWT (via TokenManager)** — auth header on API calls. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityPendingClaimsBinding`. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity`, LinearLayoutManager, annotations.
- **Retrofit**: Call, Callback, Response.
- **Project**: `PendingClaimAdapter`, `PendingClaimResponse`, `Claim`, `ClaimReviewRequest`, `ReClaimApiService`, `RetrofitClient`, `TokenManager`, `AppDataRefresh`, `ConnectionToasts`.

## Fields
- `private ActivityPendingClaimsBinding binding` — views for the pending-claims layout.
- `private ReClaimApiService apiService` — backend API client.
- `private PendingClaimAdapter adapter` — list of pending claims with review buttons.

## Functions

### `private void loadPendingClaims()`
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
- **What it does:** Fetches pending claims on the user’s items and updates the Adapter; closes the screen if there is no auth token.
- **Where it's called:** From `onResume`, from `AppDataRefresh.observeNew` (registered in `onCreate`), and after a successful `reviewClaim`.
- **Why it's written this way:** Finishing without a token avoids a useless empty screen. Network failure uses `ConnectionToasts` so offline noise is reduced.
- **Step by step:**
  1. Read auth header; if missing, `finish()` and return.
  2. Enqueue `getPendingClaimsOnMyItems`.
  3. On success with a body, `adapter.updateClaims`.
  4. On HTTP error, toast load-failed; on failure, `ConnectionToasts.showIfOnline`.

### `private void reviewClaim(String claimId, String status)`
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
- **What it does:** Sends Approve (`APPROVED`) or Reject (`REJECTED`) for one claim, then reloads the list on success.
- **Where it's called:** From the Adapter’s `ReviewListener` in `onCreate` (`onApprove` / `onReject`).
- **Why it's written this way:** Reloading the whole list after review keeps the UI in sync with the server (including siblings removed by an approve) without local list surgery.
- **Step by step:**
  1. Require auth header.
  2. Enqueue `reviewClaim` with `ClaimReviewRequest(status)`.
  3. On success: toast reviewed, call `loadPendingClaims`.
  4. On error/failure: toast review-failed.

### `protected void onCreate(Bundle savedInstanceState)`
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
- **What it does:** Inflates the layout, sets up toolbar back, wires the Adapter to review methods, and observes app-data refresh.
- **Where it's called:** By Android when this Activity starts (declared in `AndroidManifest.xml`). No other Java file currently builds an Intent to this class; home uses `PendingClaimsSheet` instead.
- **Why it's written this way:** Same review API as the bottom sheet, but as a standalone screen. Refresh observer keeps the list current if data changes elsewhere.
- **Step by step:**
  1. Inflate binding and set content.
  2. Get API service; configure toolbar with up navigation → `finish`.
  3. Create Adapter whose approve/reject call `reviewClaim`.
  4. Attach RecyclerView layout manager + adapter.
  5. Observe `AppDataRefresh` to reload.

### `protected void onResume()`
```java
@Override
protected void onResume() {
    super.onResume();
    loadPendingClaims();
}
```
- **What it does:** Reloads pending claims whenever the screen becomes visible again.
- **Where it's called:** By the Android Activity lifecycle.
- **Why it's written this way:** Ensures fresh data after leaving and returning (e.g. after another screen changed claims).
- **Step by step:**
  1. Call `super.onResume()`.
  2. Call `loadPendingClaims()`.
