# ReportHistoryActivity.java

## Overview
This is the report/claim history **Activity** (see `docs/GLOSSARY.md`). It loads the user’s own reports and the claims they submitted, merges them into `HistoryAdapter` rows with status badges, and lets the user open the other party’s contact info for approved claims.

## Concepts used here
- **Activity** — full history screen. See `docs/GLOSSARY.md`.
- **RecyclerView / Adapter** — `HistoryAdapter` + `Entry`. See `docs/GLOSSARY.md`.
- **Retrofit Call / Callback** — parallel fetches for items and claims. See `docs/GLOSSARY.md`.
- **JWT (TokenManager)** — auth header. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityReportHistoryBinding`. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX / AppCompat**: AppCompatActivity, LinearLayoutManager.
- **Retrofit**: Call, Callback, Response.
- **Project**: `HistoryAdapter`, `Item`, `Claim`, `ContactResponse`, `ContactInfoDialog`, `AppDataRefresh`, API/token helpers.

## Fields
- `private ActivityReportHistoryBinding binding` — history screen views.
- `private ReClaimApiService apiService` — API client.
- `private HistoryAdapter adapter` — combined history list.
- `private final List<HistoryAdapter.Entry> reportEntries` — rows built from `getMyItems`.
- `private final List<HistoryAdapter.Entry> claimEntries` — rows built from `getMyClaims`.
- `private int pendingRequests` — countdown so the UI draws only after both API calls finish.

## Functions

### `private HistoryAdapter.Status statusForReport(Item item)`
```java
private HistoryAdapter.Status statusForReport(Item item) {
    return "CLOSED".equalsIgnoreCase(item.getStatus())
            ? HistoryAdapter.Status.APPROVED
            : HistoryAdapter.Status.PENDING;
}
```
- **What it does:** Maps a report’s Open/Closed status to history badge Approved (closed) or Pending (open).
- **Where it's called:** When building report entries in `loadHistory`.
- **Why it's written this way:** Java comment: open reports are pending; closed ones are treated as approved for the badge.
- **Step by step:**
  1. If status is CLOSED → APPROVED; else PENDING.

### `private HistoryAdapter.Status statusForClaim(Claim claim)`
```java
private HistoryAdapter.Status statusForClaim(Claim claim) {
    String status = claim.getStatus();
    if ("APPROVED".equalsIgnoreCase(status)) {
        return HistoryAdapter.Status.APPROVED;
    }
    if ("REJECTED".equalsIgnoreCase(status)) {
        return HistoryAdapter.Status.DECLINED;
    }
    return HistoryAdapter.Status.PENDING;
}
```
- **What it does:** Maps claim API status to Pending / Approved / Declined badge.
- **Where it's called:** When building claim entries in `loadHistory`.
- **Why it's written this way:** REJECTED is shown as DECLINED in the UI enum.
- **Step by step:**
  1. APPROVED → APPROVED; REJECTED → DECLINED; anything else → PENDING.

### `private void onSectionLoaded()`
```java
private void onSectionLoaded() {
    if (binding == null || isFinishing()) {
        return;
    }
    pendingRequests--;
    if (pendingRequests > 0) {
        return;
    }

    binding.progressReportHistory.setVisibility(View.GONE);

    List<HistoryAdapter.Entry> combined = new ArrayList<>();
    combined.addAll(reportEntries);
    combined.addAll(claimEntries);
    adapter.submit(combined);

    binding.textHistoryEmpty.setVisibility(
            combined.isEmpty() ? View.VISIBLE : View.GONE);
}
```
- **What it does:** Decrements the pending-request counter; when both sections finished, hides progress, submits the combined list, and toggles the empty view.
- **Where it's called:** From both `getMyItems` and `getMyClaims` success and failure callbacks.
- **Why it's written this way:** Two parallel requests should not race to draw a half-empty list; counting down to zero is a simple join.
- **Step by step:**
  1. Guard if binding gone or finishing.
  2. Decrement `pendingRequests`; return if still &gt; 0.
  3. Hide progress; combine report + claim entries; `adapter.submit`.
  4. Show empty text if combined is empty.

### `private void loadHistory()`
```java
private void loadHistory() {
    String authHeader = TokenManager.getAuthHeader(this);
    if (authHeader == null) {
        finish();
        return;
    }

    reportEntries.clear();
    claimEntries.clear();
    pendingRequests = 2;
    binding.progressReportHistory.setVisibility(View.VISIBLE);
    binding.textHistoryEmpty.setVisibility(View.GONE);

    apiService.getMyItems(authHeader).enqueue(/* build reportEntries */);
    apiService.getMyClaims(authHeader).enqueue(/* build claimEntries with contact flag */);
}
```
- **What it does:** Starts two parallel loads: own reports and own claims; each entry becomes a history row.
- **Where it's called:** From `onResume` and `AppDataRefresh.observeNew` in `onCreate`.
- **Why it's written this way:** Approved claims set `showContact` true with the claim id so the Adapter can show the contact button. Failures still call `onSectionLoaded` so the UI does not spin forever.
- **Step by step:**
  1. Require auth or finish.
  2. Clear both lists; set `pendingRequests = 2`; show progress.
  3. Enqueue `getMyItems` → add report Entries with role “reported”.
  4. Enqueue `getMyClaims` → add claim Entries with role “claimed” and contact when approved.
  5. Each callback ends with `onSectionLoaded()`.

### `private void showOwnerContact(String claimId)`
```java
private void showOwnerContact(String claimId) {
    String authHeader = TokenManager.getAuthHeader(this);
    if (authHeader == null) {
        return;
    }
    apiService.getClaimContact(authHeader, claimId)
            .enqueue(new Callback<ContactResponse>() {
                // success → ContactInfoDialog.show with reloader
            });
}
```
- **What it does:** Fetches contact for an approved claim and shows `ContactInfoDialog`, with a reloader that re-calls `getClaimContact`.
- **Where it's called:** As the `HistoryAdapter` contact click listener (`this::showOwnerContact` in `onCreate`).
- **Why it's written this way:** Contact is fetched on demand, not embedded in every history row. Reloader keeps the dialog fresh if app data refreshes.
- **Step by step:**
  1. Require auth header.
  2. Enqueue `getClaimContact`.
  3. On success (and not finishing): show dialog with lifecycle owner + reloader.
  4. On error/failure: toast contact load failed.

### `protected void onCreate(Bundle savedInstanceState)`
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // inflate, toolbar back, HistoryAdapter, AppDataRefresh.observeNew
}
```
- **What it does:** Sets up toolbar, RecyclerView, Adapter, and refresh observation.
- **Where it's called:** By Android when opened from `ProfileSettingsFragment` (Intent).
- **Why it's written this way:** Up navigation finishes the screen; refresh observer reloads when data changes elsewhere.
- **Step by step:**
  1. Inflate binding; get API service.
  2. Toolbar with home-as-up → `finish`.
  3. Create `HistoryAdapter(this::showOwnerContact)`; attach to RecyclerView.
  4. `AppDataRefresh.observeNew(this, this::loadHistory)`.

### `protected void onResume()` / `protected void onDestroy()`
```java
@Override
protected void onResume() {
    super.onResume();
    loadHistory();
}

@Override
protected void onDestroy() {
    super.onDestroy();
    binding = null;
}
```
- **What it does:** Reloads on resume; clears binding on destroy so late callbacks can bail out.
- **Where it's called:** Activity lifecycle.
- **Why it's written this way:** Fresh history each time the user returns; nulling binding avoids use-after-destroy in `onSectionLoaded`.
- **Step by step:**
  1. `onResume` → `loadHistory()`.
  2. `onDestroy` → `binding = null`.
