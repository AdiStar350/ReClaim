# MyReportAdapter.java

## Overview
This is a **RecyclerView Adapter** for the user’s own reports on the “My items” screen (see `docs/GLOSSARY.md`). Each row shows title, category, location, photo, and Lost/Found chip, plus Edit and Delete buttons. The card itself is not clickable — only those buttons fire actions.

## Concepts used here
- **RecyclerView / Adapter / ViewHolder** — feeds and recycles report rows. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemMyReportBinding` for `item_my_report.xml`. See `docs/GLOSSARY.md`.
- **Glide** — loads thumbnails. See `docs/GLOSSARY.md`.
- **Interface** — `Listener` with `onEdit` / `onDelete` so the Fragment handles actions.

## Libraries and imports
- **AndroidX**: RecyclerView, LayoutInflater, Context, annotations.
- **Glide**: image load with cross-fade.
- **Project**: `Item` model, `ItemMyReportBinding`, type chip colors/strings.

## Fields
- `private final List<Item> items` — reports currently shown.
- `private final Listener listener` — edit/delete callbacks (`MyItemsFragment` implements this).

## Functions

### `public interface Listener` — `onEdit` / `onDelete`
```java
public interface Listener {
    void onEdit(@NonNull Item item);

    void onDelete(@NonNull Item item);
}
```
- **What it does:** Declares the two actions the host must handle for a report row.
- **Where it's called:** Implemented by `MyItemsFragment`; invoked from button click listeners in `bind`.
- **Why it's written this way:** The Adapter only displays; delete/edit logic (dialogs, API) stays in the Fragment.
- **Step by step:**
  1. `onEdit` receives the item to open for editing.
  2. `onDelete` receives the item to delete.

### `public MyReportAdapter(Listener listener)`
```java
public MyReportAdapter(@NonNull Listener listener) {
    this.listener = listener;
}
```
- **What it does:** Stores the listener; list starts empty.
- **Where it's called:** `MyItemsFragment` constructs it with `this` (the Fragment implements `Listener`).
- **Why it's written this way:** Same pattern as other adapters — listener required at construction.
- **Step by step:**
  1. Assign `listener`.

### `public void updateItems(List<Item> newItems)`
```java
public void updateItems(@NonNull List<Item> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
}
```
- **What it does:** Replaces the list and refreshes the RecyclerView.
- **Where it's called:** From `MyItemsFragment` after fetching / mutating the user’s reports.
- **Why it's written this way:** Full refresh is simple for a personal list that is not huge.
- **Step by step:**
  1. Clear, addAll, `notifyDataSetChanged()`.

### `public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)`
```java
@NonNull
@Override
public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemMyReportBinding binding = ItemMyReportBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
}
```
- **What it does:** Inflates one “my report” row and wraps it in a ViewHolder.
- **Where it's called:** By RecyclerView when it needs a new row.
- **Why it's written this way:** ViewBinding keeps view access typed.
- **Step by step:**
  1. Inflate `ItemMyReportBinding`.
  2. Return `new ViewHolder(binding)`.

### `public void onBindViewHolder(ViewHolder holder, int position)` / `getItemCount()`
```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(items.get(position));
}

@Override
public int getItemCount() {
    return items.size();
}
```
- **What it does:** Binds the item at `position`; reports how many rows exist.
- **Where it's called:** By RecyclerView during scrolling and updates.
- **Why it's written this way:** Standard Adapter split — count vs bind.
- **Step by step:**
  1. `onBindViewHolder` calls `holder.bind(items.get(position))`.
  2. `getItemCount` returns `items.size()`.

### `ViewHolder` / `void bind(Item item)`
```java
void bind(@NonNull Item item) {
    Context context = binding.getRoot().getContext();
    binding.textTitle.setText(item.getTitle());
    binding.textCategory.setText(item.getCategory());
    binding.textLocation.setText(item.getLocation());

    Glide.with(context)
            .load(item.getImageUrl())
            .placeholder(R.drawable.ic_item_placeholder)
            .error(R.drawable.ic_item_placeholder)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(binding.imageThumbnail);

    String type = item.getType() != null ? item.getType() : "";
    if ("Lost".equalsIgnoreCase(type)) {
        binding.chipType.setText(R.string.type_lost);
        binding.chipType.setChipBackgroundColorResource(R.color.type_lost);
    } else {
        binding.chipType.setText(type.isEmpty() ? "" : context.getString(R.string.type_found));
        binding.chipType.setChipBackgroundColorResource(R.color.type_found);
    }
    binding.chipType.setTextColor(context.getColor(R.color.text_on_accent));

    binding.btnEdit.setOnClickListener(v -> listener.onEdit(item));
    binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    binding.getRoot().setOnClickListener(null);
    binding.getRoot().setClickable(false);
}
```
- **What it does:** Fills texts, image, and type chip; wires Edit/Delete; clears any root click so the card is not tappable.
- **Where it's called:** From `onBindViewHolder`.
- **Why it's written this way:** Comments in the Java note that only buttons should act — a stray card tap must not open edit. Disabling root click avoids that.
- **Step by step:**
  1. Set title, category, location.
  2. Load thumbnail with Glide.
  3. Style Lost/Found chip.
  4. Hook edit/delete to `listener`.
  5. Clear root click listener and set `clickable` false.
