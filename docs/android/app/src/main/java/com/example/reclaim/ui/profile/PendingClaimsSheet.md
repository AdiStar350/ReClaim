# PendingClaimsSheet.java

## Overview
This is a helper that shows a Material **BottomSheetDialog** listing every pending claim the signed-in owner still needs to review. Unlike `PendingClaimsActivity`, it lives on top of a host **Fragment** (Home), can confirm reject, and after Approve it opens `ContactInfoDialog` with the claimant’s contact info.

## Concepts used here
- **BottomSheetDialog / BottomSheetBehavior** — sheet that expands over the current screen.
- **Fragment** — host that must stay added while network callbacks run. See `docs/GLOSSARY.md`.
- **RecyclerView / Adapter** — `PendingClaimAdapter`. See `docs/GLOSSARY.md`.
- **Retrofit Call / Callback** — review claim and fetch contact. See `docs/GLOSSARY.md`.
- **JWT (TokenManager)** — auth header. See `docs/GLOSSARY.md`.

## Libraries and imports
- **Material**: BottomSheetDialog, BottomSheetBehavior, MaterialAlertDialogBuilder.
- **AndroidX**: Fragment, LinearLayoutManager, annotations.
- **Retrofit**: Call, Callback, Response.
- **Project**: `PendingClaimAdapter`, `PendingClaimResponse`, `Claim`, `ClaimReviewRequest`, `ContactResponse`, `ContactInfoDialog`, `TokenManager`, `RetrofitClient`.

## Fields
- `private final Fragment host` — Fragment that owns the sheet (must be added for UI/API).
- `private final Listener listener` — notified when the list changes or the sheet dismisses.
- `private final BottomSheetDialog dialog` — the sheet window.
- `private final DialogPendingClaimsBinding binding` — sheet layout.
- `private final PendingClaimAdapter adapter` — claim rows.
- `private final ReClaimApiService apiService` — API client.
- `private boolean notifyHostOnDismiss` — whether dismiss should call `onSheetDismissed`.
- `private boolean reviewing` — true while an approve/reject request is in flight (blocks `replaceClaims`).

## Functions

### `public interface Listener` — `onClaimsChanged` / `onSheetDismissed`
```java
public interface Listener {
    void onClaimsChanged(@NonNull List<PendingClaimResponse> remaining);

    void onSheetDismissed();
}
```
- **What it does:** Lets Home keep its pending-claims card/count in sync and clear its sheet reference when closed.
- **Where it's called:** Implemented in `HomeFragment.showPendingClaimsSheet`; invoked from `afterListChanged` and the dialog dismiss listener.
- **Why it's written this way:** The sheet does not own Home’s LiveData; it reports changes upward.
- **Step by step:**
  1. `onClaimsChanged` receives the remaining list after a review.
  2. `onSheetDismissed` runs when the user closes the sheet (unless dismissed quietly).

### `private PendingClaimsSheet(Fragment host, Listener listener)` (constructor)
```java
private PendingClaimsSheet(@NonNull Fragment host, @NonNull Listener listener) {
    // inflate binding, create adapter with approve/reject, size list, build dialog
}
```
- **What it does:** Builds adapter (approve → `reviewClaim`, reject → `confirmReject`), sets list height to ~55% of screen, creates the BottomSheetDialog.
- **Where it's called:** Only from static `show`.
- **Why it's written this way:** Private constructor forces callers through `show`, which also loads claims and displays the dialog. Nested scrolling + fixed height keeps the list usable inside the sheet.
- **Step by step:**
  1. Store host/listener; get API service.
  2. Inflate binding; create Adapter with review listeners.
  3. Configure RecyclerView; set height to 55% of display pixels.
  4. Create dialog, set content, expand on show, notify dismiss.

### `private void expandSheet()`
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
- **What it does:** Forces the sheet open fully and skips the half-collapsed state.
- **Where it's called:** From the dialog’s `setOnShowListener`.
- **Why it's written this way:** Pending claims should be readable immediately without an extra drag.
- **Step by step:**
  1. Find Material’s `design_bottom_sheet` FrameLayout.
  2. Get behavior; skip collapsed; set EXPANDED.

### `private void updateHeader()`
```java
private void updateHeader() {
    binding.textPendingClaimsHeader.setText(host.getString(
            R.string.pending_claims_sheet_title, adapter.getItemCount()));
}
```
- **What it does:** Sets the header text to include the current claim count.
- **Where it's called:** From `replaceClaims` and `afterListChanged`.
- **Why it's written this way:** Keeps the title count accurate as rows are removed.
- **Step by step:**
  1. Format the title string with `adapter.getItemCount()`.
  2. Set it on the header TextView.

### `private void confirmReject(PendingClaimResponse claim)`
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
- **What it does:** Asks the owner to confirm before rejecting.
- **Where it's called:** From the Adapter’s `onReject`.
- **Why it's written this way:** Reject is destructive for the claimant; a confirm dialog reduces accidents. Approve goes straight to `reviewClaim`.
- **Step by step:**
  1. Abort if host Fragment not added.
  2. Show Material confirm dialog.
  3. On positive, call `reviewClaim(claim, false)`.

### `private void afterListChanged()`
```java
private void afterListChanged() {
    updateHeader();
    listener.onClaimsChanged(adapter.getClaims());
    if (adapter.getItemCount() == 0) {
        dismiss();
    }
}
```
- **What it does:** Refreshes header, notifies host, and auto-closes when no claims remain.
- **Where it's called:** After a successful review in `reviewClaim`.
- **Why it's written this way:** Empty sheet is useless; dismissing returns focus to Home.
- **Step by step:**
  1. Update header.
  2. `listener.onClaimsChanged` with remaining claims.
  3. If count is 0, `dismiss()`.

### `private void revealClaimantContact(String claimId)`
```java
private void revealClaimantContact(@Nullable String claimId) {
    // getClaimContact → ContactInfoDialog.show with reloader and claimant title
}
```
- **What it does:** Fetches contact for the approved claim and shows `ContactInfoDialog` titled as claimant contact, with an optional reload hook.
- **Where it's called:** From `reviewClaim`’s success path when `approve` is true.
- **Why it's written this way:** Approving means the owner should see how to reach the claimant. Reloader re-fetches if `AppDataRefresh` ticks while the dialog is open.
- **Step by step:**
  1. Require claim id and host added + auth header.
  2. Enqueue `getClaimContact`.
  3. On success: `ContactInfoDialog.show` with reloader that calls `getClaimContact` again.
  4. On failure: toast contact unavailable.

### `private void reviewClaim(PendingClaimResponse claim, boolean approve)`
```java
private void reviewClaim(PendingClaimResponse claim, boolean approve) {
    // set reviewing + busy; reviewClaim API; on approve removeClaimsForItem + contact;
    // on reject removeClaim; then afterListChanged
}
```
- **What it does:** Sends APPROVED or REJECTED, marks the Adapter busy, updates the list locally on success, then notifies the host.
- **Where it's called:** From approve listener and from confirm-reject positive button.
- **Why it's written this way:** `setBusyClaimId` disables buttons during the request. Approve removes all claims for that item (siblings); reject removes only that claim. Local removal is snappier than a full reload.
- **Step by step:**
  1. Guard host/claim id/auth.
  2. Set `reviewing = true` and `adapter.setBusyClaimId`.
  3. Enqueue `reviewClaim` with the status string.
  4. On failure: clear busy, toast.
  5. On success approve: reveal contact, `removeClaimsForItem`; on reject: `removeClaim`.
  6. Clear busy/reviewing; `afterListChanged`.

### `public void replaceClaims(List<PendingClaimResponse> claims)`
```java
public void replaceClaims(@NonNull List<PendingClaimResponse> claims) {
    if (reviewing) {
        return;
    }
    adapter.updateClaims(new ArrayList<>(claims));
    updateHeader();
}
```
- **What it does:** Replaces the list from outside (Home sync), unless a review is in progress.
- **Where it's called:** From static `show` and from `HomeFragment.syncPendingClaimsSheet`.
- **Why it's written this way:** Ignoring updates while `reviewing` avoids wiping the list mid-request.
- **Step by step:**
  1. If reviewing, return.
  2. Copy claims into Adapter; update header.

### `public static PendingClaimsSheet show(Fragment host, List claims, Listener listener)`
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
- **What it does:** Factory that builds, fills, shows, and returns the sheet instance.
- **Where it's called:** `HomeFragment.showPendingClaimsSheet`.
- **Why it's written this way:** Callers keep the returned instance to dismiss/sync later.
- **Step by step:**
  1. Construct sheet.
  2. `replaceClaims`.
  3. Show dialog; return sheet.

### `public boolean isShowing()` / `public void dismiss()` / `public void dismissQuietly()` / `private void dismiss(boolean notifyHost)`
```java
public boolean isShowing() {
    return dialog.isShowing();
}

public void dismiss() {
    dismiss(true);
}

public void dismissQuietly() {
    dismiss(false);
}

private void dismiss(boolean notifyHost) {
    notifyHostOnDismiss = notifyHost;
    if (dialog.isShowing()) {
        dialog.dismiss();
    }
}
```
- **What it does:** Reports visibility; dismisses with or without calling `onSheetDismissed`.
- **Where it's called:** `isShowing` / `dismissQuietly` / `dismiss` from `HomeFragment` sync/show logic; `dismiss` also from `afterListChanged` when empty.
- **Why it's written this way:** Quiet dismiss lets Home replace the sheet without treating it as a user close.
- **Step by step:**
  1. Set `notifyHostOnDismiss`.
  2. If showing, `dialog.dismiss()`.
