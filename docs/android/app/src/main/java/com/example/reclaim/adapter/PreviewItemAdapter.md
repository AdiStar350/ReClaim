# PreviewItemAdapter.java

## Overview
This is a **RecyclerView Adapter** for the small preview cards on the home screen (see `docs/GLOSSARY.md`). Each row shows a title, location, Lost/Found chip, and photo. Tapping a card notifies a click listener with that `Item`.

## Concepts used here
- **RecyclerView / Adapter / ViewHolder** — the Adapter feeds rows; a ViewHolder holds one row’s views for reuse. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemCardPreviewBinding` gives typed access to `item_card_preview.xml`. See `docs/GLOSSARY.md`.
- **Glide** — loads the item image URL into the ImageView. See `docs/GLOSSARY.md`.
- **Interface** — `OnItemClickListener` lets the host Fragment react to taps without the Adapter knowing the screen class.

## Libraries and imports
- **AndroidX**: `RecyclerView`, LayoutInflater / ViewGroup for creating rows, nullability annotations.
- **Glide**: image loading with placeholder, error drawable, and a short cross-fade.
- **Project**: `Item` model, `ItemCardPreviewBinding`, string/color resources for Lost/Found chips.

## Fields
- `private final List<Item> items` — the current list of items shown in the RecyclerView.
- `private final OnItemClickListener listener` — callback when a card is tapped.

## Functions

### `public interface OnItemClickListener` — `void onItemClick(Item item)`
```java
public interface OnItemClickListener {
    void onItemClick(@NonNull Item item);
}
```
- **What it does:** Declares the one method the host must implement when a preview card is tapped.
- **Where it's called:** Implemented by `HomeFragment` (lambda passed to the constructor); invoked from `ViewHolder.bind` when the root view is clicked.
- **Why it's written this way:** Keeps the Adapter reusable — it only knows “tell someone an Item was clicked.”
- **Step by step:**
  1. The interface names `onItemClick` with the tapped `Item`.
  2. The host supplies an implementation at construction time.

### `public PreviewItemAdapter(OnItemClickListener listener)`
```java
public PreviewItemAdapter(@NonNull OnItemClickListener listener) {
    this.listener = listener;
}
```
- **What it does:** Stores the click listener; the item list starts empty.
- **Where it's called:** `HomeFragment` creates it for the nearby-preview RecyclerView.
- **Why it's written this way:** Listener is required so every card has somewhere to report taps.
- **Step by step:**
  1. Assign `listener` to the field.
  2. Leave `items` as an empty `ArrayList`.

### `public void updateItems(List<Item> newItems)`
```java
public void updateItems(@NonNull List<Item> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
}
```
- **What it does:** Replaces the in-memory list and redraws the whole RecyclerView.
- **Where it's called:** From `HomeFragment` after nearby/search preview data is ready.
- **Why it's written this way:** Simple full refresh is enough for a short home preview list.
- **Step by step:**
  1. Clear `items`.
  2. Add all of `newItems`.
  3. Call `notifyDataSetChanged()`.

### `public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)`
```java
@NonNull
@Override
public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemCardPreviewBinding binding = ItemCardPreviewBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
}
```
- **What it does:** Inflates one preview card layout and wraps it in a ViewHolder.
- **Where it's called:** By RecyclerView when it needs a new row view.
- **Why it's written this way:** ViewBinding inflation avoids `findViewById` and matches the project’s other adapters.
- **Step by step:**
  1. Inflate `ItemCardPreviewBinding` with the parent’s context.
  2. Return a new `ViewHolder` holding that binding.

### `public void onBindViewHolder(ViewHolder holder, int position)`
```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(items.get(position));
}
```
- **What it does:** Fills the row at `position` with that list item.
- **Where it's called:** By RecyclerView when a row scrolls on screen or data changes.
- **Why it's written this way:** Binding logic lives in `ViewHolder.bind` so `onBindViewHolder` stays one line.
- **Step by step:**
  1. Get `items.get(position)`.
  2. Call `holder.bind(...)`.

### `public int getItemCount()`
```java
@Override
public int getItemCount() {
    return items.size();
}
```
- **What it does:** Tells RecyclerView how many rows there are.
- **Where it's called:** By RecyclerView during layout and updates.
- **Why it's written this way:** Standard Adapter contract.
- **Step by step:**
  1. Return `items.size()`.

### `ViewHolder` constructor and `void bind(Item item)`
```java
ViewHolder(@NonNull ItemCardPreviewBinding binding) {
    super(binding.getRoot());
    this.binding = binding;
}

void bind(@NonNull Item item) {
    android.content.Context ctx = binding.getRoot().getContext();

    binding.textItemTitle.setText(item.getTitle());
    binding.textItemLocation.setText(item.getLocation());

    String type = item.getType() != null ? item.getType() : "";
    if ("Lost".equalsIgnoreCase(type)) {
        binding.chipType.setText(R.string.type_lost);
        binding.chipType.setChipBackgroundColorResource(R.color.type_lost);
    } else {
        binding.chipType.setText(type.isEmpty() ? "" : ctx.getString(R.string.type_found));
        binding.chipType.setChipBackgroundColorResource(R.color.type_found);
    }
    binding.chipType.setTextColor(ctx.getColor(R.color.text_on_accent));

    Glide.with(ctx)
            .load(item.getImageUrl())
            .placeholder(R.drawable.ic_item_placeholder)
            .error(R.drawable.ic_item_placeholder)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(binding.imageItem);

    binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
}
```
- **What it does:** Stores the binding; `bind` fills title, location, Lost/Found chip, image, and click listener for one `Item`.
- **Where it's called:** Constructor from `onCreateViewHolder`; `bind` from `onBindViewHolder`.
- **Why it's written this way:** Non-Lost types show as Found (or blank if type is empty). Glide handles missing URLs with a placeholder. Click goes through the listener so HomeFragment can open details.
- **Step by step:**
  1. Constructor: keep the binding and pass the root to `RecyclerView.ViewHolder`.
  2. Set title and location texts.
  3. Style the type chip for Lost vs Found.
  4. Load the image with Glide.
  5. Set the root click listener to call `listener.onItemClick(item)`.
