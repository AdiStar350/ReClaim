# ItemDetailsActivity.java

## Overview
This is the item-details **Activity** (see `docs/GLOSSARY.md`). It shows one lost or found report (photo, description, location, date, reporter, status) and a bottom action: claim a found item (with a verification question) or, for a lost item, open a locked “I found this” report form. Owners see no claim button.

## Concepts used here
- **Activity** — full details screen. See `docs/GLOSSARY.md`.
- **Intent extras** — item fields passed from `ItemNavigationHelper` or notifications.
- **Retrofit Call / Callback** — refresh item, submit claim, (unused helper for owner contact). See `docs/GLOSSARY.md`.
- **JWT (TokenManager)** — auth for API calls. See `docs/GLOSSARY.md`.
- **Glide** — item photo and reporter avatar. See `docs/GLOSSARY.md`.
- **Activity Result API** — waits for `ReportActivity` RESULT_OK after a found report for a lost item.

## Libraries and imports
- **AndroidX / AppCompat**: AppCompatActivity, AlertDialog, Activity Result contracts.
- **Material**: MaterialAlertDialogBuilder, TextInputLayout/EditText for the claim dialog.
- **Glide**: images.
- **Retrofit**: Call, Callback, Response.
- **Project**: `Item`, `Claim`, `ClaimRequest`, `ContactResponse`, `ContactInfoDialog`, `ReportActivity`, `AppStateEvents`, `TokenManager`, `ConnectionToasts`.

## Fields
- Public `EXTRA_*` constants — Intent keys for id, title, description, location, status, category, image, type, verification question, owner id/name/avatar, reported-at, already-interacted flag.
- `binding`, `apiService` — views and API.
- `itemId`, `verificationQuestion`, `ownerId`, `itemType` — core item state for actions.
- `claimSubmitting` — true while a claim request is in flight.
- `hasUserAlreadyInteracted` — claim/found-report already done (from Intent, server, or `AppStateEvents`).
- `foundReportLauncher` — starts `ReportActivity` for “I found this” and marks interacted on RESULT_OK.

## Functions

### `private String formatReportedDate(String isoTimestamp)`
```java
private String formatReportedDate(String isoTimestamp) {
    // parse yyyy-MM-dd'T'HH:mm:ss UTC → "MMM d, yyyy"; fallbacks for empty/bad input
}
```
- **What it does:** Formats an ISO-ish timestamp into a short local date string.
- **Where it's called:** From `bindFields` for the date TextView.
- **Why it's written this way:** Truncates to 19 chars before parse; on failure shows first 10 chars or the raw string; empty → placeholder.
- **Step by step:**
  1. Empty → placeholder.
  2. Try parse UTC; format with default locale.
  3. On exception, return date prefix or raw text.

### `private boolean isCurrentUserOwner()`
```java
private boolean isCurrentUserOwner() {
    if (TextUtils.isEmpty(ownerId)) {
        return false;
    }
    String currentUserId = TokenManager.getUserId(this);
    return !TextUtils.isEmpty(currentUserId) && ownerId.equals(currentUserId);
}
```
- **What it does:** True if the report’s owner id matches the signed-in user id.
- **Where it's called:** From `setupBottomAction` to hide the claim container for owners.
- **Why it's written this way:** Owners should not claim their own items.
- **Step by step:**
  1. False if owner id empty.
  2. Compare to `TokenManager.getUserId`.

### `private void bindFields(...)`
```java
private void bindFields(@Nullable String title, ... ownerProfileUrl) {
    // set texts, chips, Glide for avatar + item image with placeholders
}
```
- **What it does:** Fills every detail field from string values (Intent or server Item).
- **Where it's called:** From `populateFromIntent` and `applyItem`.
- **Why it's written this way:** Shared binding so Intent paint and server refresh look the same. Missing values use string placeholders.
- **Step by step:**
  1. Set title, description, location, formatted date, reporter name.
  2. Load reporter avatar and item image with Glide.
  3. Set status chip (OPEN/CLOSED/raw) and category chip.

### `private void showInteractedButton()` / `private void markInteractedLocally()`
```java
private void showInteractedButton() {
    // disabled button: "report submitted" or "claim pending"
}

private void markInteractedLocally() {
    hasUserAlreadyInteracted = true;
    if (!TextUtils.isEmpty(itemId)) {
        AppStateEvents.markItemInteracted(itemId);
    }
    setupBottomAction();
}
```
- **What it does:** Shows a non-tappable bottom button after the user already claimed or filed a found report; persists that fact in `AppStateEvents`.
- **Where it's called:** `showInteractedButton` from `setupBottomAction`; `markInteractedLocally` after successful claim, after found-report RESULT_OK, and paths that need instant UI update.
- **Why it's written this way:** Immediate UI feedback without waiting for another fetch; `AppStateEvents` survives leaving the screen.
- **Step by step:**
  1. Set interacted flag and mark in AppStateEvents.
  2. `setupBottomAction` → disabled label based on LOST vs found.

### `private void setupBottomAction()`
```java
private void setupBottomAction() {
    if (claimSubmitting) return;
    if (isCurrentUserOwner()) { hide container; return; }
    // if interacted → showInteractedButton
    // LOST → "I found this" → openFoundReportForLostItem
    // else → Claim → showClaimDialog
}
```
- **What it does:** Configures the bottom button by ownership, prior interaction, and lost vs found type.
- **Where it's called:** From `onCreate` (via setup after populate), `applyItem`, `markInteractedLocally`, and when ending claim loading.
- **Why it's written this way:** Lost items need a found report linked to the lost id; found items use the claim dialog. Skips rewiring while `claimSubmitting`.
- **Step by step:**
  1. Abort if submitting; hide if owner.
  2. If already interacted, show disabled button.
  3. LOST → found-report launcher path; otherwise claim dialog.

### `private void openFoundReportForLostItem()`
```java
private void openFoundReportForLostItem() {
    Intent intent = new Intent(this, ReportActivity.class);
    intent.putExtra(ReportActivity.EXTRA_LOCK_FOUND_TYPE, true);
    intent.putExtra(ReportActivity.EXTRA_TARGET_LOST_REPORT_ID, itemId);
    foundReportLauncher.launch(intent);
}
```
- **What it does:** Opens ReportActivity locked to Found type and targeting this lost report id.
- **Where it's called:** Bottom button click when `itemType` is LOST.
- **Why it's written this way:** Launcher marks interacted only if the found report finishes with RESULT_OK.
- **Step by step:**
  1. Require item id.
  2. Put lock-found + target-lost extras; launch for result.

### `private void loadReporterContact()`
```java
private void loadReporterContact() {
    // getLostItemOwnerContact → ContactInfoDialog.show with reloader
}
```
- **What it does:** Would fetch the lost-item owner’s contact and show `ContactInfoDialog`.
- **Where it's called:** No callers found in the project — the method is defined but unused.
- **Why it's written this way:** Same dialog/reloader pattern as history/pending claims; likely kept for a contact affordance that is not wired yet.
- **Step by step:**
  1. Require auth + item id; disable claim button while loading.
  2. Enqueue `getLostItemOwnerContact`; show dialog or toast; re-enable button.

### `private void showClaimDialog()` / `private void submitClaimToBackend(String validationAnswer)` / `handleClaimError` / `setClaimButtonLoading`
```java
private void showClaimDialog() { /* verification answer dialog; submitClaimToBackend */ }

private void submitClaimToBackend(String validationAnswer) {
    // ClaimRequest → submitClaim; success → markInteractedLocally
}

private void handleClaimError(int statusCode) { /* 400/401/404/5xx toasts */ }

private void setClaimButtonLoading(boolean isLoading) {
    claimSubmitting = isLoading;
    // submitting label or setupBottomAction()
}
```
- **What it does:** Asks for the verification answer, posts the claim, maps HTTP errors to toasts, and toggles button loading state.
- **Where it's called:** Claim button → dialog → submit; callbacks call error/loading helpers.
- **Why it's written this way:** Positive button click is set in `OnShowListener` so an empty answer can keep the dialog open. Loading disables double-submit.
- **Step by step:**
  1. Dialog shows question; empty answer sets field error.
  2. Submit builds `ClaimRequest(itemId, answer)` and enqueues.
  3. Success: toast, clear submitting, `markInteractedLocally`.
  4. Errors: `handleClaimError` by status; failure uses `ConnectionToasts`.

### `private void populateFromIntent()` / `private void refreshFromServer()` / `private void applyItem(Item item)`
```java
private void populateFromIntent() { /* read EXTRA_* into fields + bindFields */ }

private void refreshFromServer() {
    apiService.getItemById(...).enqueue(/* applyItem on success */);
}

private void applyItem(@NonNull Item item) {
    // update fields from Item, bindFields, setupBottomAction
}
```
- **What it does:** First paint from Intent extras; then optionally refresh from `getItemById` and rebind.
- **Where it's called:** `populateFromIntent` from `onCreate`; `refreshFromServer` from `onResume` and `AppStateEvents.observeNew`; `applyItem` from refresh success.
- **Why it's written this way:** Intent extras make the screen usable immediately (e.g. from notifications); server refresh corrects stale data. Failure keeps what was shown.
- **Step by step:**
  1. Read extras including interacted flag OR `AppStateEvents.wasItemInteracted`.
  2. Refresh requires item id + auth; on success `applyItem`.

### `private void setupToolbar()` / `onCreate` / `onResume` / `onSupportNavigateUp`
```java
private void setupToolbar() {
    setSupportActionBar(...);
    binding.collapsingToolbar.setTitleEnabled(false);
}

@Override
protected void onCreate(Bundle savedInstanceState) {
    // inflate, setupToolbar, populateFromIntent, setupBottomAction, observe AppStateEvents
}

@Override
protected void onResume() {
    super.onResume();
    refreshFromServer();
}

@Override
public boolean onSupportNavigateUp() {
    getOnBackPressedDispatcher().onBackPressed();
    return true;
}
```
- **What it does:** Standard screen setup; refreshes on resume; up navigation uses the back dispatcher.
- **Where it's called:** Lifecycle / system up affordance. Opened via `ItemNavigationHelper.createDetailsIntent` (Home, Search, etc.) and notification intents in `AppNotifications`.
- **Why it's written this way:** Collapsing toolbar title disabled so the title under the photo is the main title.
- **Step by step:**
  1. Inflate; get API; toolbar with home-as-up.
  2. Populate from Intent; wire bottom action; observe AppStateEvents for refresh.
  3. Resume refreshes; up triggers back.
