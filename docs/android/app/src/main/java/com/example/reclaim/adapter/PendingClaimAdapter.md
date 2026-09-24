# PendingClaimAdapter.java

## Overview
This is a **RecyclerView Adapter** for claims waiting on the item owner’s Approve/Reject decision (see `docs/GLOSSARY.md`). Each row shows the report title, claimant name, their validation answer, a thumbnail, and the two review buttons. Used by `PendingClaimsSheet`.

## Concepts used here
- **RecyclerView / Adapter / ViewHolder** — list of pending claim cards. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemPendingClaimBinding`. See `docs/GLOSSARY.md`.
- **Glide** — loads the report thumbnail. See `docs/GLOSSARY.md`.
- **DTO** — `PendingClaimResponse` is the API-shaped claim waiting for review. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX**: RecyclerView, LayoutInflater, TextUtils, annotations.
- **Glide**: thumbnail loading.
- **Project**: `PendingClaimResponse`, `ItemPendingClaimBinding`, placeholder strings.

## Fields
- `private final List<PendingClaimResponse> claims` — current pending claims.
- `private final ReviewListener listener` — approve/reject callbacks.
- `private String busyClaimId` — when non-null, Approve/Reject buttons are disabled on every row (a review request is in flight). Note: the field is stored but the bind logic only checks whether it is null, not whether it matches a specific claim.

## Functions

### `public interface ReviewListener` — `onApprove` / `onReject`
```java
public interface ReviewListener {
    void onApprove(PendingClaimResponse claim);

    void onReject(PendingClaimResponse claim);
}
```
- **What it does:** Declares the two review actions the host must implement.
- **Where it's called:** Implemented in `PendingClaimsSheet`; invoked from button listeners in `bind`.
- **Why it's written this way:** Network/review logic stays in the Activity/Sheet, not in the Adapter.
- **Step by step:**
  1. Host implements both methods.
  2. Buttons call the matching method with that row’s claim.

### `public PendingClaimAdapter(ReviewListener listener)`
```java
public PendingClaimAdapter(ReviewListener listener) {
    this.listener = listener;
}
```
- **What it does:** Stores the review listener.
- **Where it's called:** `PendingClaimsSheet` when the sheet is set up.
- **Why it's written this way:** Listener is required for the buttons to do anything.
- **Step by step:**
  1. Assign `listener`.

### `public void updateClaims(List<PendingClaimResponse> newClaims)`
```java
public void updateClaims(List<PendingClaimResponse> newClaims) {
    claims.clear();
    if (newClaims != null) {
        claims.addAll(newClaims);
    }
    notifyDataSetChanged();
}
```
- **What it does:** Replaces the list (null becomes empty) and redraws.
- **Where it's called:** From `PendingClaimsSheet` after loading claims from the API.
- **Why it's written this way:** Null-safe so a failed/empty response does not crash.
- **Step by step:**
  1. Clear `claims`.
  2. If `newClaims` is non-null, add them.
  3. `notifyDataSetChanged()`.

### `public List<PendingClaimResponse> getClaims()`
```java
@NonNull
public List<PendingClaimResponse> getClaims() {
    return Collections.unmodifiableList(new ArrayList<>(claims));
}
```
- **What it does:** Returns a read-only copy of the current claims.
- **Where it's called:** Grep shows no external callers in Java beyond the class itself being used; hosts may rely on their own lists. The method exists on the Adapter for safe read access without mutating internals.
- **Why it's written this way:** Unmodifiable copy prevents outside code from changing the Adapter’s list by accident.
- **Step by step:**
  1. Copy `claims` into a new list.
  2. Wrap with `Collections.unmodifiableList` and return.

### `public void setBusyClaimId(String claimId)`
```java
public void setBusyClaimId(@Nullable String claimId) {
    this.busyClaimId = claimId;
    if (!claims.isEmpty()) {
        notifyItemRangeChanged(0, claims.size());
    }
}
```
- **What it does:** Stores a “busy” claim id and rebinds all rows so buttons enable/disable.
- **Where it's called:** From `PendingClaimsSheet` around approve/reject network calls.
- **Why it's written this way:** Disabling all buttons while one review is in flight stops double-taps; rebinding every row refreshes enabled state.
- **Step by step:**
  1. Set `busyClaimId`.
  2. If there are claims, `notifyItemRangeChanged` for the whole range.

### `public boolean removeClaim(String claimId)`
```java
public boolean removeClaim(@Nullable String claimId) {
    if (claimId == null) {
        return false;
    }
    for (int i = 0; i < claims.size(); i++) {
        if (claimId.equals(claims.get(i).getClaimId())) {
            claims.remove(i);
            notifyItemRemoved(i);
            return true;
        }
    }
    return false;
}
```
- **What it does:** Finds one claim by id, removes that row with an animated removal, returns whether it found it.
- **Where it's called:** From pending-claims UI after a successful reject (or similar single-claim removal).
- **Why it's written this way:** `notifyItemRemoved` animates better than a full dataset refresh.
- **Step by step:**
  1. If id is null, return false.
  2. Loop for matching `getClaimId()`.
  3. Remove at index, notify, return true; else return false.

### `public void removeClaimsForItem(String itemId)`
```java
public void removeClaimsForItem(@Nullable String itemId) {
    if (itemId == null) {
        return;
    }
    for (int i = claims.size() - 1; i >= 0; i--) {
        if (itemId.equals(claims.get(i).getItemId())) {
            claims.remove(i);
            notifyItemRemoved(i);
        }
    }
}
```
- **What it does:** Removes every pending claim that belongs to the same report (item), including siblings auto-rejected when one claim is approved.
- **Where it's called:** From pending-claims UI after a successful approve (all claims for that item should leave the list).
- **Why it's written this way:** Walks backward so removing while iterating stays safe; comment in Java notes auto-rejected siblings.
- **Step by step:**
  1. If `itemId` is null, return.
  2. From last index to 0, remove matching `getItemId()` and notify each removal.

### `onCreateViewHolder` / `onBindViewHolder` / `getItemCount`
```java
@NonNull
@Override
public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemPendingClaimBinding binding = ItemPendingClaimBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
}

@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(claims.get(position));
}

@Override
public int getItemCount() {
    return claims.size();
}
```
- **What it does:** Standard create/bind/count for pending-claim rows.
- **Where it's called:** By RecyclerView.
- **Why it's written this way:** Same pattern as other adapters.
- **Step by step:**
  1. Inflate binding into ViewHolder.
  2. Bind claim at position.
  3. Return list size.

### `ViewHolder.bind(PendingClaimResponse claim)`
```java
void bind(PendingClaimResponse claim) {
    Context context = binding.getRoot().getContext();

    String title = !TextUtils.isEmpty(claim.getItemTitle())
            ? claim.getItemTitle()
            : context.getString(R.string.placeholder_untitled_report);
    binding.textItemTitle.setText(title);

    String claimantName = !TextUtils.isEmpty(claim.getClaimantName())
            ? claim.getClaimantName()
            : context.getString(R.string.pending_claim_unknown_claimant);
    binding.textClaimant.setText(context.getString(R.string.label_claimant, claimantName));

    String answer = claim.getValidationAnswer() != null
            ? claim.getValidationAnswer() : "";
    binding.textValidationAnswer.setText(
            context.getString(R.string.label_claim_answer, answer));

    String imageUrl = claim.getItemImageUrl();
    Glide.with(context)
            .load(!TextUtils.isEmpty(imageUrl) ? imageUrl : null)
            .centerCrop()
            .placeholder(R.drawable.ic_item_placeholder)
            .error(R.drawable.ic_item_placeholder)
            .into(binding.imageItemThumbnail);

    boolean enabled = busyClaimId == null;
    binding.btnApproveClaim.setEnabled(enabled);
    binding.btnRejectClaim.setEnabled(enabled);
    binding.btnApproveClaim.setOnClickListener(v -> listener.onApprove(claim));
    binding.btnRejectClaim.setOnClickListener(v -> listener.onReject(claim));
}
```
- **What it does:** Fills title/claimant/answer/image; enables buttons only when no review is busy; wires Approve/Reject.
- **Where it's called:** From `onBindViewHolder`.
- **Why it's written this way:** Placeholders cover missing title/name. Empty image URL loads `null` so Glide shows the placeholder. Buttons disable for the whole list while `busyClaimId` is set.
- **Step by step:**
  1. Choose title / claimant / answer strings (with fallbacks).
  2. Load thumbnail with Glide.
  3. Enable buttons iff `busyClaimId == null`.
  4. Set click listeners to `listener.onApprove` / `onReject`.
