# HistoryAdapter.java

## Overview
This is a **RecyclerView Adapter** for report/claim history rows (see `docs/GLOSSARY.md`). Each row shows a role label (e.g. report vs claim), title, thumbnail, a status badge (Pending / Approved / Declined), and optionally a “view contact” button when an approved claim has a claim id.

## Concepts used here
- **RecyclerView / Adapter / ViewHolder** — history list. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemHistoryBinding`. See `docs/GLOSSARY.md`.
- **Glide** — loads history thumbnails. See `docs/GLOSSARY.md`.
- **Enum** — `Status` bundles label and badge colors for each state.
- **Nested static class** — `Entry` is the Adapter’s own row model (not a network DTO).

## Libraries and imports
- **AndroidX**: RecyclerView, ContextCompat, ColorStateList, View, annotations (`@StringRes`, `@ColorRes`).
- **Glide**: image loading.
- **Project**: `ItemHistoryBinding`, history status strings/colors.

## Fields
- `private final List<Entry> entries` — rows currently shown.
- `private final ContactClickListener contactClickListener` — called when “view contact” is tapped.

## Functions

### `public interface ContactClickListener` — `void onViewContact(String claimId)`
```java
public interface ContactClickListener {
    void onViewContact(@NonNull String claimId);
}
```
- **What it does:** Declares the callback for opening contact info for a claim.
- **Where it's called:** `ReportHistoryActivity` passes `this::showOwnerContact`; invoked from the contact button in `HistoryViewHolder.bind`.
- **Why it's written this way:** Contact fetch/dialog stays in the Activity.
- **Step by step:**
  1. Host implements `onViewContact`.
  2. Button calls it with `entry.claimId`.

### `public enum Status`
```java
public enum Status {
    PENDING(R.string.history_status_pending,
            R.color.status_pending_bg, R.color.status_pending_text),
    APPROVED(R.string.history_status_approved,
            R.color.status_open_bg, R.color.status_open_text),
    DECLINED(R.string.history_status_declined,
            R.color.status_closed_bg, R.color.status_closed_text);
    // fields + constructor assign labelRes, bgColorRes, textColorRes
}
```
- **What it does:** Maps each history status to a string resource and badge background/text colors.
- **Where it's called:** `ReportHistoryActivity` picks a `Status` when building `Entry` objects; `bind` reads the enum fields to style the badge.
- **Why it's written this way:** Keeps colors/labels next to the status name so Activity code stays short.
- **Step by step:**
  1. Each constant stores label + bg + text color resource ids.
  2. Constructor assigns those three fields.

### `public static class Entry` constructors
```java
public Entry(String title, String imageUrl,
             @StringRes int roleLabelRes, Status status) {
    this(title, imageUrl, roleLabelRes, status, null, false);
}

public Entry(String title, String imageUrl,
             @StringRes int roleLabelRes, Status status,
             String claimId, boolean showContact) {
    this.title = title;
    this.imageUrl = imageUrl;
    this.roleLabelRes = roleLabelRes;
    this.status = status;
    this.claimId = claimId;
    this.showContact = showContact;
}
```
- **What it does:** Builds one history row; the short constructor hides contact (no claim id).
- **Where it's called:** `ReportHistoryActivity` when turning reports and claims into list entries.
- **Why it's written this way:** Report rows need no contact button; approved claims pass `claimId` and `showContact = true`.
- **Step by step:**
  1. Short form delegates to the long form with `null` claim id and `showContact = false`.
  2. Long form stores all six fields.

### `public HistoryAdapter(ContactClickListener contactClickListener)`
```java
public HistoryAdapter(ContactClickListener contactClickListener) {
    this.contactClickListener = contactClickListener;
}
```
- **What it does:** Stores the contact-click listener.
- **Where it's called:** `ReportHistoryActivity` in `onCreate`.
- **Why it's written this way:** Listener is optional at bind time (null-checked) but usually provided.
- **Step by step:**
  1. Assign the field.

### `public void submit(List<Entry> newEntries)`
```java
public void submit(List<Entry> newEntries) {
    entries.clear();
    if (newEntries != null) {
        entries.addAll(newEntries);
    }
    notifyDataSetChanged();
}
```
- **What it does:** Replaces the list and redraws.
- **Where it's called:** From `ReportHistoryActivity` after combining report and claim entries.
- **Why it's written this way:** Null-safe full refresh.
- **Step by step:**
  1. Clear, optionally addAll, notify.

### `onCreateViewHolder` / `onBindViewHolder` / `getItemCount`
```java
@NonNull
@Override
public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemHistoryBinding binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new HistoryViewHolder(binding, contactClickListener);
}

@Override
public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
    holder.bind(entries.get(position));
}

@Override
public int getItemCount() {
    return entries.size();
}
```
- **What it does:** Creates holders (passing the contact listener), binds entries, reports count.
- **Where it's called:** By RecyclerView.
- **Why it's written this way:** Listener is passed into the ViewHolder so `bind` can wire the button.
- **Step by step:**
  1. Inflate binding, construct `HistoryViewHolder`.
  2. Bind `entries.get(position)`.
  3. Return size.

### `HistoryViewHolder.bind(Entry entry)`
```java
void bind(Entry entry) {
    Context context = binding.getRoot().getContext();

    binding.textHistoryRole.setText(entry.roleLabelRes);
    binding.textHistoryTitle.setText(
            !TextUtils.isEmpty(entry.title) ? entry.title
                    : context.getString(R.string.placeholder_untitled_report));

    Glide.with(context)
            .load(!TextUtils.isEmpty(entry.imageUrl) ? entry.imageUrl : null)
            .centerCrop()
            .placeholder(R.drawable.ic_item_placeholder)
            .error(R.drawable.ic_item_placeholder)
            .into(binding.imageHistory);

    binding.textHistoryStatus.setText(entry.status.labelRes);
    binding.textHistoryStatus.setBackgroundTintList(ColorStateList.valueOf(
            ContextCompat.getColor(context, entry.status.bgColorRes)));
    binding.textHistoryStatus.setTextColor(
            ContextCompat.getColor(context, entry.status.textColorRes));

    boolean showContact = entry.showContact && !TextUtils.isEmpty(entry.claimId);
    binding.btnHistoryContact.setVisibility(showContact ? View.VISIBLE : View.GONE);
    if (showContact && listener != null) {
        binding.btnHistoryContact.setOnClickListener(
                v -> listener.onViewContact(entry.claimId));
    } else {
        binding.btnHistoryContact.setOnClickListener(null);
    }
}
```
- **What it does:** Fills role, title, image, status badge colors, and shows/hides the contact button.
- **Where it's called:** From `onBindViewHolder`.
- **Why it's written this way:** Contact only appears when both `showContact` and a non-empty `claimId` are set; otherwise the listener is cleared so recycled rows do not keep a stale click.
- **Step by step:**
  1. Set role and title (placeholder if blank).
  2. Load image with Glide.
  3. Apply status label and tint/text colors from the enum.
  4. Show contact button only when allowed; wire or clear the click listener.
