```java
void onClaimsChanged(@NonNull List<PendingClaimResponse> remaining);
```

What that function does: I tell the host the remaining pending claims after a review.
What the function is responsible for: I am the list-changed callback.
Where it works: afterListChanged calls me. HomeFragment implements me.
Why I chose to write the code of the function the way its written: Home can hide its card when the list is empty.
More: I pass adapter.getClaims().

```java
void onSheetDismissed();
```

What that function does: I tell the host the sheet closed.
What the function is responsible for: I am the dismiss callback.
Where it works: The dialog dismiss listener calls me unless dismissQuietly was used. HomeFragment implements me.
Why I chose to write the code of the function the way its written: Quiet dismiss lets Home replace the sheet without a loop.
More: I take no arguments.

```java
private PendingClaimsSheet(@NonNull Fragment host, @NonNull Listener listener) {
    this.host = host;
    this.listener = listener;
    this.apiService = RetrofitClient.getApiService();

    Context context = host.requireContext();
    binding = DialogPendingClaimsBinding.inflate(LayoutInflater.from(context));
    adapter = new PendingClaimAdapter(new PendingClaimAdapter.ReviewListener() {
        @Override
        public void onApprove(PendingClaimResponse claim) {
            reviewClaim(claim, true);
        }

        @Override
        public void onReject(PendingClaimResponse claim) {
            confirmReject(claim);
        }
    });

    binding.recyclerPendingClaims.setLayoutManager(new LinearLayoutManager(context));
    binding.recyclerPendingClaims.setAdapter(adapter);
    binding.recyclerPendingClaims.setNestedScrollingEnabled(true);
    ViewGroup.LayoutParams listParams = binding.recyclerPendingClaims.getLayoutParams();
    listParams.height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.55f);
    binding.recyclerPendingClaims.setLayoutParams(listParams);

    dialog = new BottomSheetDialog(context);
    dialog.setContentView(binding.getRoot());
    dialog.setOnShowListener(d -> expandSheet());
    dialog.setOnDismissListener(d -> {
        if (notifyHostOnDismiss) {
            listener.onSheetDismissed();
        }
    });
}
```

What that function does: I build the bottom sheet, the list, and the review callbacks.
What the function is responsible for: I am the private constructor. show() is the public entry.
Where it works: show() constructs me. I inflate dialog_pending_claims.xml.
Why I chose to write the code of the function the way its written: 55% screen height keeps the list scrollable. Reject asks first. Approve goes straight to review.
More: I use BottomSheetDialog and PendingClaimAdapter.

```java
public void onApprove(PendingClaimResponse claim) {
    reviewClaim(claim, true);
}
```

What that function does: I approve that claim without an extra confirm.
What the function is responsible for: I am the adapter approve callback.
Where it works: PendingClaimAdapter.bind calls me.
Why I chose to write the code of the function the way its written: Approve also shows contact, so I skip a second dialog.
More: I call reviewClaim(true).

```java
public void onReject(PendingClaimResponse claim) {
    confirmReject(claim);
}
```

What that function does: I ask before rejecting.
What the function is responsible for: I am the adapter reject callback.
Where it works: PendingClaimAdapter.bind calls me.
Why I chose to write the code of the function the way its written: Reject is harder to undo, so I confirm.
More: I call confirmReject.

```java
@NonNull
public static PendingClaimsSheet show(@NonNull Fragment host,
                                      @NonNull List<PendingClaimResponse> claims,
                                      @NonNull Listener listener) {
    PendingClaimsSheet sheet = new PendingClaimsSheet(host, listener);
    sheet.replaceClaims(claims);
    sheet.dialog.show();
    return sheet;
}
```

What that function does: I create the sheet, fill it, and show it.
What the function is responsible for: I am the public open method.
Where it works: HomeFragment calls me.
Why I chose to write the code of the function the way its written: Returning the instance lets Home dismiss or replace it later.
More: I call replaceClaims then dialog.show.

```java
public boolean isShowing() {
    return dialog.isShowing();
}
```

What that function does: I return whether the sheet is on screen.
What the function is responsible for: I let Home avoid opening a second sheet.
Where it works: HomeFragment checks me.
Why I chose to write the code of the function the way its written: I forward to BottomSheetDialog.
More: I use dialog.isShowing.

```java
public void dismiss() {
    dismiss(true);
}
```

What that function does: I close the sheet and tell the host.
What the function is responsible for: I am the normal dismiss.
Where it works: afterListChanged calls me when the list is empty.
Why I chose to write the code of the function the way its written: notifyHost true is the default.
More: I call dismiss(true).

```java
public void dismissQuietly() {
    dismiss(false);
}
```

What that function does: I close the sheet without telling the host.
What the function is responsible for: I stop a dismiss loop when Home replaces me.
Where it works: HomeFragment can call me before opening a new sheet.
Why I chose to write the code of the function the way its written: Quiet skip of onSheetDismissed avoids extra reloads.
More: I call dismiss(false).

```java
private void dismiss(boolean notifyHost) {
    notifyHostOnDismiss = notifyHost;
    if (dialog.isShowing()) {
        dialog.dismiss();
    }
}
```

What that function does: I set whether to notify, then I dismiss if showing.
What the function is responsible for: I am the shared close path.
Where it works: I am called by dismiss and dismissQuietly.
Why I chose to write the code of the function the way its written: The flag is read in the dismiss listener.
More: I use dialog.dismiss.

```java
public void replaceClaims(@NonNull List<PendingClaimResponse> claims) {
    if (reviewing) {
        return;
    }
    adapter.updateClaims(new ArrayList<>(claims));
    updateHeader();
}
```

What that function does: I replace the list unless a review is in flight.
What the function is responsible for: I refresh the sheet from Home without closing it.
Where it works: show() and HomeFragment call me.
Why I chose to write the code of the function the way its written: Skipping while reviewing stops a refresh from wiping the busy state.
More: I copy the list into the adapter.

```java
private void expandSheet() {
    FrameLayout bottomSheet = dialog.findViewById(
            com.google.android.material.R.id.design_bottom_sheet);
    if (bottomSheet == null) {
        return;
    }
    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
    behavior.setSkipCollapsed(true);
    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
}
```

What that function does: I expand the sheet fully and skip the half-open state.
What the function is responsible for: I make the list usable as soon as it shows.
Where it works: The dialog onShow listener calls me.
Why I chose to write the code of the function the way its written: SkipCollapsed stops a peek that hides the buttons.
More: I use BottomSheetBehavior.STATE_EXPANDED.

```java
private void updateHeader() {
    binding.textPendingClaimsHeader.setText(host.getString(
            R.string.pending_claims_sheet_title, adapter.getItemCount()));
}
```

What that function does: I put the remaining count in the header.
What the function is responsible for: I keep the title in sync with the list.
Where it works: I am called from replaceClaims and afterListChanged.
Why I chose to write the code of the function the way its written: The string takes the count as an argument.
More: I use adapter.getItemCount.

```java
private void confirmReject(PendingClaimResponse claim) {
    if (!host.isAdded()) {
        return;
    }
    new MaterialAlertDialogBuilder(host.requireContext())
            .setTitle(R.string.claim_reject_confirm_title)
            .setMessage(R.string.claim_reject_confirm_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_reject_claim, (d, w) -> reviewClaim(claim, false))
            .show();
}
```

What that function does: I ask before sending REJECTED.
What the function is responsible for: I am the reject confirm dialog.
Where it works: I am called from onReject.
Why I chose to write the code of the function the way its written: host.isAdded stops a dialog after the fragment left.
More: I use MaterialAlertDialogBuilder.

```java
private void reviewClaim(PendingClaimResponse claim, boolean approve) {
    if (!host.isAdded() || claim.getClaimId() == null) {
        return;
    }
    String authHeader = TokenManager.getAuthHeader(host.requireContext());
    if (authHeader == null) {
        return;
    }

    reviewing = true;
    adapter.setBusyClaimId(claim.getClaimId());
    String status = approve ? "APPROVED" : "REJECTED";
    apiService.reviewClaim(authHeader, claim.getClaimId(), new ClaimReviewRequest(status))
            .enqueue(new Callback<Claim>() {
                @Override
                public void onResponse(@NonNull Call<Claim> call,
                                       @NonNull Response<Claim> response) {
                    if (!host.isAdded()) {
                        return;
                    }
                    if (!response.isSuccessful()) {
                        reviewing = false;
                        adapter.setBusyClaimId(null);
                        Toast.makeText(host.requireContext(),
                                R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (approve) {
                        revealClaimantContact(claim.getClaimId());
                        adapter.removeClaimsForItem(claim.getItemId());
                    } else {
                        adapter.removeClaim(claim.getClaimId());
                    }
                    reviewing = false;
                    adapter.setBusyClaimId(null);
                    afterListChanged();
                }

                @Override
                public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
                    if (!host.isAdded()) {
                        return;
                    }
                    reviewing = false;
                    adapter.setBusyClaimId(null);
                    Toast.makeText(host.requireContext(),
                            R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
                }
            });
}
```

What that function does: I send approve or reject, lock the buttons, then I update the list.
What the function is responsible for: I am PUT review from the sheet.
Where it works: I am called from onApprove and confirmReject.
Why I chose to write the code of the function the way its written: Approve removes every pending claim on that item because the server auto-rejects siblings. Reject removes only that row.
More: I use adapter.setBusyClaimId and ClaimReviewRequest.

```java
public void onResponse(@NonNull Call<Claim> call,
                       @NonNull Response<Claim> response) {
    if (!host.isAdded()) {
        return;
    }
    if (!response.isSuccessful()) {
        reviewing = false;
        adapter.setBusyClaimId(null);
        Toast.makeText(host.requireContext(),
                R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
        return;
    }
    if (approve) {
        revealClaimantContact(claim.getClaimId());
        adapter.removeClaimsForItem(claim.getItemId());
    } else {
        adapter.removeClaim(claim.getClaimId());
    }
    reviewing = false;
    adapter.setBusyClaimId(null);
    afterListChanged();
}
```

What that function does: I show claimant contact after approve, remove rows, then I tell Home.
What the function is responsible for: I am the Retrofit success path for review from the sheet.
Where it works: Retrofit calls me from reviewClaim.
Why I chose to write the code of the function the way its written: Contact comes right after approve so the owner can reach the claimant.
More: I call revealClaimantContact and afterListChanged.

```java
public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
    if (!host.isAdded()) {
        return;
    }
    reviewing = false;
    adapter.setBusyClaimId(null);
    Toast.makeText(host.requireContext(),
            R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
}
```

What that function does: I unlock the buttons and toast that review failed.
What the function is responsible for: I am the Retrofit failure path for review from the sheet.
Where it works: Retrofit calls me from reviewClaim.
Why I chose to write the code of the function the way its written: reviewing false lets replaceClaims work again.
More: I use setBusyClaimId(null).

```java
private void afterListChanged() {
    updateHeader();
    listener.onClaimsChanged(adapter.getClaims());
    if (adapter.getItemCount() == 0) {
        dismiss();
    }
}
```

What that function does: I refresh the header, tell Home, and close if nothing is left.
What the function is responsible for: I am the post-review UI update.
Where it works: I am called after a successful review.
Why I chose to write the code of the function the way its written: An empty sheet should close itself.
More: I use listener.onClaimsChanged.

```java
private void revealClaimantContact(@Nullable String claimId) {
    if (claimId == null || !host.isAdded()) {
        return;
    }
    String authHeader = TokenManager.getAuthHeader(host.requireContext());
    if (authHeader == null) {
        return;
    }

    apiService.getClaimContact(authHeader, claimId)
            .enqueue(new Callback<ContactResponse>() {
                @Override
                public void onResponse(@NonNull Call<ContactResponse> call,
                                       @NonNull Response<ContactResponse> response) {
                    if (!host.isAdded()) {
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        ContactInfoDialog.show(host.requireContext(), response.body(),
                                host.getViewLifecycleOwner(),
                                callback -> {
                                    String header = TokenManager.getAuthHeader(
                                            host.requireContext());
                                    if (header == null) {
                                        return;
                                    }
                                    apiService.getClaimContact(header, claimId)
                                            .enqueue(callback);
                                },
                                R.string.contact_claimant_title);
                    } else {
                        Toast.makeText(host.requireContext(),
                                R.string.msg_contact_unavailable, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ContactResponse> call,
                                      @NonNull Throwable t) {
                    if (!host.isAdded()) {
                        return;
                    }
                    Toast.makeText(host.requireContext(),
                            R.string.msg_contact_unavailable, Toast.LENGTH_SHORT).show();
                }
            });
}
```

What that function does: I load claimant contact after approve and show it with a claimant title.
What the function is responsible for: I am the post-approve contact reveal.
Where it works: I am called from reviewClaim when approve is true.
Why I chose to write the code of the function the way its written: contact_claimant_title makes it clear this is the claimant, not the owner.
More: I use ContactInfoDialog.show with a reloader.

```java
public void onResponse(@NonNull Call<ContactResponse> call,
                       @NonNull Response<ContactResponse> response) {
    if (!host.isAdded()) {
        return;
    }
    if (response.isSuccessful() && response.body() != null) {
        ContactInfoDialog.show(host.requireContext(), response.body(),
                host.getViewLifecycleOwner(),
                callback -> {
                    String header = TokenManager.getAuthHeader(
                            host.requireContext());
                    if (header == null) {
                        return;
                    }
                    apiService.getClaimContact(header, claimId)
                            .enqueue(callback);
                },
                R.string.contact_claimant_title);
    } else {
        Toast.makeText(host.requireContext(),
                R.string.msg_contact_unavailable, Toast.LENGTH_SHORT).show();
    }
}
```

What that function does: I show the claimant dialog, or I toast that contact is unavailable.
What the function is responsible for: I am the Retrofit success path for getClaimContact from the sheet.
Where it works: Retrofit calls me from revealClaimantContact.
Why I chose to write the code of the function the way its written: host.isAdded stops a dialog after leave.
More: I use ContactInfoDialog.

```java
public void onFailure(@NonNull Call<ContactResponse> call,
                      @NonNull Throwable t) {
    if (!host.isAdded()) {
        return;
    }
    Toast.makeText(host.requireContext(),
            R.string.msg_contact_unavailable, Toast.LENGTH_SHORT).show();
}
```

What that function does: I toast that contact could not load.
What the function is responsible for: I am the Retrofit failure path for getClaimContact from the sheet.
Where it works: Retrofit calls me from revealClaimantContact.
Why I chose to write the code of the function the way its written: The approve already succeeded, so I only warn about contact.
More: I use msg_contact_unavailable.
