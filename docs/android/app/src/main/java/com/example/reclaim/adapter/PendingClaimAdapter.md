```java
void onApprove(PendingClaimResponse claim);
```

What that function does: I am the approve callback the host must implement.
What the function is responsible for: I tell the screen which claim to approve.
Where it works: ViewHolder.bind calls me. PendingClaimsActivity and PendingClaimsSheet implement me.
Why I chose to write the code of the function the way its written: An interface keeps the adapter free of API calls.
More: I take PendingClaimResponse.

```java
void onReject(PendingClaimResponse claim);
```

What that function does: I am the reject callback the host must implement.
What the function is responsible for: I tell the screen which claim to reject.
Where it works: ViewHolder.bind calls me. PendingClaimsActivity and PendingClaimsSheet implement me.
Why I chose to write the code of the function the way its written: Approve and reject stay two methods so the host can confirm reject separately.
More: I take PendingClaimResponse.

```java
public PendingClaimAdapter(ReviewListener listener) {
    this.listener = listener;
}
```

What that function does: I store the review listener.
What the function is responsible for: I am the adapter constructor.
Where it works: I am constructed in PendingClaimsActivity and PendingClaimsSheet.
Why I chose to write the code of the function the way its written: I start with an empty list and fill it later with updateClaims.
More: I keep claims as an ArrayList.

```java
public void updateClaims(List<PendingClaimResponse> newClaims) {
    claims.clear();
    if (newClaims != null) {
        claims.addAll(newClaims);
    }
    notifyDataSetChanged();
}
```

What that function does: I replace the list and redraw every row.
What the function is responsible for: I am the bulk refresh after a fetch.
Where it works: I am called by PendingClaimsActivity and PendingClaimsSheet.
Why I chose to write the code of the function the way its written: A null list becomes empty so the UI does not crash.
More: I use notifyDataSetChanged.

```java
@NonNull
public List<PendingClaimResponse> getClaims() {
    return Collections.unmodifiableList(new ArrayList<>(claims));
}
```

What that function does: I return a copy of the current claims that cannot be edited.
What the function is responsible for: I let the host read remaining claims after a review.
Where it works: I am called by PendingClaimsSheet when reporting remaining claims.
Why I chose to write the code of the function the way its written: An unmodifiable copy stops the host from changing my list behind my back.
More: I use Collections.unmodifiableList.

```java
public void setBusyClaimId(@Nullable String claimId) {
    this.busyClaimId = claimId;
    if (!claims.isEmpty()) {
        notifyItemRangeChanged(0, claims.size());
    }
}
```

What that function does: I remember which claim is in flight and disable the buttons.
What the function is responsible for: I stop a second tap while a review request runs.
Where it works: I am called by PendingClaimsActivity and PendingClaimsSheet around reviewClaim.
Why I chose to write the code of the function the way its written: I redraw all rows so every approve/reject pair updates. A null id means idle.
More: I use notifyItemRangeChanged.

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

What that function does: I remove one claim row with a list animation.
What the function is responsible for: I drop a claim after a successful review.
Where it works: I am called by PendingClaimsActivity and PendingClaimsSheet after review succeeds.
Why I chose to write the code of the function the way its written: notifyItemRemoved animates one row instead of redrawing the whole list.
More: I match on getClaimId.

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

What that function does: I remove every pending claim for one report.
What the function is responsible for: I drop sibling claims that the server auto-rejects after an approve.
Where it works: I am called by PendingClaimsSheet after an approve.
Why I chose to write the code of the function the way its written: I walk backwards so remove does not skip indexes.
More: I match on getItemId and notifyItemRemoved.

```java
@NonNull
@Override
public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemPendingClaimBinding binding = ItemPendingClaimBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
}
```

What that function does: I inflate item_pending_claim for a new row.
What the function is responsible for: I am RecyclerView's create path.
Where it works: RecyclerView calls me. The layout is item_pending_claim.xml.
Why I chose to write the code of the function the way its written: View Binding keeps findViewById out of the holder.
More: I use ItemPendingClaimBinding.inflate.

```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(claims.get(position));
}
```

What that function does: I bind one claim to a row.
What the function is responsible for: I am RecyclerView's bind path.
Where it works: RecyclerView calls me for each visible row.
Why I chose to write the code of the function the way its written: The holder owns the field mapping.
More: I call ViewHolder.bind.

```java
@Override
public int getItemCount() {
    return claims.size();
}
```

What that function does: I return how many pending claims I hold.
What the function is responsible for: I tell RecyclerView the list size.
Where it works: RecyclerView calls me.
Why I chose to write the code of the function the way its written: Size matches the in-memory list.
More: I return claims.size().

```java
ViewHolder(ItemPendingClaimBinding binding) {
    super(binding.getRoot());
    this.binding = binding;
}
```

What that function does: I store the row binding.
What the function is responsible for: I am the row constructor.
Where it works: I am created in onCreateViewHolder.
Why I chose to write the code of the function the way its written: super(binding.getRoot()) is what RecyclerView needs.
More: I keep ItemPendingClaimBinding.

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

What that function does: I fill title, claimant, answer, photo, and the two buttons.
What the function is responsible for: I am one pending-claim card.
Where it works: I am called from onBindViewHolder. Clicks go to ReviewListener.
Why I chose to write the code of the function the way its written: I disable both buttons while any review is busy. Empty titles get a placeholder.
More: I use Glide, TextUtils, and string resources for labels.
