# HomeFragment.java

## Overview
This is the home **Fragment** on the dashboard (see `docs/GLOSSARY.md`). It greets the user, previews nearby open items, shows possible matches for the user’s open lost reports, and surfaces cards for pending claims to review, targeted found reports, and unacknowledged claim decisions (approve/decline).

## Concepts used here
- **Fragment** — home tab content inside `DashboardActivity`. See `docs/GLOSSARY.md`.
- **ViewModel / LiveData** — shared `ItemsViewModel` for the public item feed. See `docs/GLOSSARY.md`.
- **RecyclerView / Adapter** — horizontal `PreviewItemAdapter` for nearby; dynamic `ItemAdapter` rows for matches. See `docs/GLOSSARY.md`.
- **Retrofit Call / Callback** — greeting, my items, pending claims, targeted found, claim decisions. See `docs/GLOSSARY.md`.
- **DashboardNavigator** — “show more” jumps to the Search tab.
- **Fused location** — optional last location for nearby preview ordering.

## Libraries and imports
- **AndroidX**: Fragment, ViewModelProvider, RecyclerView, permissions.
- **Google location**: FusedLocationProviderClient.
- **Glide**: home avatar.
- **Material**: MaterialAlertDialogBuilder, MaterialTextView headers.
- **Retrofit**: Call, Callback, Response.
- **Project**: adapters, `ItemMatchHelper`, `ItemNavigationHelper`, `PendingClaimsSheet`, `ContactInfoDialog`, `AppStateEvents`, `IncomingClaimEvents`, `ClaimDecisionEvents`, API/token helpers.

## Fields
- `NEARBY_PREVIEW_LIMIT` (4) — max cards in the nearby strip.
- `binding`, `viewModel`, `apiService`, `nearbyAdapter`, `fusedLocationClient`.
- `userLatitude` / `userLongitude` — for nearby ranking when permission granted.
- `myItems` — user’s reports (for match groups).
- `targetedFoundReports`, `pendingClaims`, `unacknowledgedDecisions` — card data sources.
- `claimDecisionDialog`, `decidingClaimId`, `pendingClaimsSheet` — open UI pieces to sync/dismiss.

## Functions

### `private void renderTargetedFoundCard()` / `private void renderClaimCards()` / `private void renderDecisionCard()`
```java
private void renderTargetedFoundCard() { /* show/hide card + subtitle from first targeted found */ }
private void renderClaimCards() { /* show pending count or hide */ }
private void renderDecisionCard() { /* approved/declined title + item title */ }
```
- **What it does:** Updates visibility and text for the three home alert cards from in-memory lists.
- **Where it's called:** After the matching load methods finish (and when the pending sheet reports list changes).
- **Why it's written this way:** Cards stay hidden when empty so Home stays clean.
- **Step by step:**
  1. Targeted: visible if list non-empty; subtitle uses first item’s title.
  2. Pending: subtitle with count.
  3. Decision: approved vs declined title from first unacknowledged claim.

### `private void loadTargetedFoundReports()` / `loadPendingClaims()` / `loadClaimDecisions()`
```java
// each: auth header → enqueue API → store list → render (+ sync sheet/dialog)
```
- **What it does:** Fetches targeted found reports, pending claims on my items, and the user’s claims filtered to unacknowledged APPROVED/REJECTED decisions.
- **Where it's called:** From `refreshFromServer`; pending/decisions also from `IncomingClaimEvents` / `ClaimDecisionEvents` observers.
- **Why it's written this way:** Failure clears the list and still re-renders so stale cards disappear. Decision load skips already-acknowledged claims.
- **Step by step:**
  1. Require auth; enqueue the matching API.
  2. On response, replace the list (or empty on failure).
  3. Call render (+ `syncPendingClaimsSheet` / `syncClaimDecisionDialog`).

### `private void showPendingClaimsSheet()` / `syncPendingClaimsSheet()`
```java
private void showPendingClaimsSheet() {
    // show or replaceClaims on PendingClaimsSheet; on dismiss refreshFromServer
}

private void syncPendingClaimsSheet() {
    // dismiss if empty; else replaceClaims while showing
}
```
- **What it does:** Opens the pending-claims bottom sheet; keeps it in sync when the list reloads.
- **Where it's called:** Card click → show; load callbacks → sync.
- **Why it's written this way:** If the sheet is already open, replace claims instead of stacking dialogs. Dismiss triggers a full home refresh.
- **Step by step:**
  1. Show only if claims exist.
  2. Listener updates local `pendingClaims` and re-renders the card.
  3. Sync dismisses when empty or pushes new data into the open sheet.

### `private void showClaimDecisionDialog(Claim claim)` / `syncClaimDecisionDialog()` / `acknowledgeDecision` / `showOwnerContact`
```java
private void showClaimDecisionDialog(Claim claim) {
    // confirm acknowledge; if approved, neutral "view contact" → showOwnerContact
}

private void acknowledgeDecision(String claimId) {
    // optimistic remove + acknowledgeClaimDecision API → reload
}

private void showOwnerContact(String claimId) {
    // getClaimContact → ContactInfoDialog
}
```
- **What it does:** Shows the decision popup; confirm acknowledges on the server; approved decisions can open owner contact without closing first (neutral button).
- **Where it's called:** Decision card click; sync when decisions reload; confirm button; contact button.
- **Why it's written this way:** Optimistic remove makes the card disappear quickly; reload corrects if the API fails. Contact uses OnShowListener so the dialog stays open.
- **Step by step:**
  1. Build dialog; store `decidingClaimId`.
  2. Sync updates title/message or dismisses if that claim left the list.
  3. Acknowledge removes locally then POSTs acknowledge; contact fetches and shows dialog.

### `private void loadUserGreeting()` / `loadAvatar` / `loadMyItems` / `fetchLocationIfAllowed`
```java
private void loadUserGreeting() { /* getCurrentUser → named greeting + avatar */ }
private void loadAvatar(@Nullable String url) { /* Glide circle crop */ }
private void loadMyItems() { /* getMyItems → myItems; renderMatches */ }
private void fetchLocationIfAllowed() { /* last location if permission; renderNearby */ }
```
- **What it does:** Personalizes greeting/avatar; loads the user’s items for match grouping; optionally reads GPS for nearby preview.
- **Where it's called:** Greeting/my items from `refreshFromServer`; location from `onViewCreated`; avatar from greeting success.
- **Why it's written this way:** No permission request here — only uses location if already granted. Match rendering needs both public feed and `myItems`.
- **Step by step:**
  1. Greeting updates text if name present; loads avatar URL.
  2. My items stored then `renderMatches` with current ViewModel items.
  3. If fine location granted, store coords and re-render nearby.

### `private void observeItems()` / `renderNearby` / `renderMatches`
```java
private void observeItems() {
    // loading → progress; error → ConnectionToasts; items → renderNearby + renderMatches
}

private void renderNearby(List<Item> items) {
    // ItemMatchHelper.nearbyPreview(..., NEARBY_PREVIEW_LIMIT) → PreviewItemAdapter
}

private void renderMatches(List<Item> allItems) {
    // open LOST myItems → ItemMatchHelper.buildMatchGroups → headers + ItemAdapter recyclers
}
```
- **What it does:** Observes the shared items feed; fills nearby preview and dynamic match sections.
- **Where it's called:** `observeItems` from `onViewCreated`; renders from observers, location fetch, and my-items load.
- **Why it's written this way:** Match groups are built in code (header TextView + nested RecyclerView) because each lost item can have its own match list. Nearby uses a horizontal preview of at most 4.
- **Step by step:**
  1. Nearby: helper picks closest/open items; toggle empty/show-more.
  2. Matches: collect open LOST from `myItems`; build groups excluding own items via user id; inflate headers + adapters that open details.

### `private void refreshFromServer()`
```java
private void refreshFromServer() {
    viewModel.loadItems();
    loadMyItems();
    loadUserGreeting();
    loadPendingClaims();
    loadTargetedFoundReports();
    loadClaimDecisions();
}
```
- **What it does:** Reloads every home data source.
- **Where it's called:** `onResume`, `AppStateEvents.observeNew`, pending sheet dismiss.
- **Why it's written this way:** One entry point keeps home consistent after navigation or push-driven events.
- **Step by step:**
  1. Kick off ViewModel load plus the five home-specific loaders.

### `onCreateView` / `onViewCreated` / `onResume` / `onDestroyView`
```java
// inflate; wire nearby list + cards; observers for AppState/IncomingClaim/ClaimDecision;
// refresh on resume; dismiss dialog/sheet on destroy
```
- **What it does:** Builds UI, wires clicks (search tab via `DashboardNavigator`, cards, details intents), observes live events, cleans up overlays.
- **Where it's called:** Fragment lifecycle; hosted by `DashboardActivity` home tab.
- **Why it's written this way:** ItemDecoration adds gutters between horizontal cards. Destroy dismisses dialog and quietly dismisses the sheet to avoid leaks.
- **Step by step:**
  1. Inflate binding; create ViewModel/API/location client and nearby adapter.
  2. Wire show-more → `selectTab(nav_search)`; card clicks; fetch location; observe items + events.
  3. Resume → `refreshFromServer`; destroy → dismiss overlays, null binding.
