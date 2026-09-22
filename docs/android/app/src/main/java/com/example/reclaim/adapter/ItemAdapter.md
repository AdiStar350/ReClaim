# ItemAdapter.java

## Overview
This is a **RecyclerView Adapter** that shows full item cards (title, category, location, Lost/Found chip, Open/Closed status, photo). Used on Search and in Home’s match groups. Tapping a card calls `OnItemClickListener.onItemClick`.

## Concepts used here
- **RecyclerView / Adapter / ViewHolder** — list of item cards. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ItemCardBinding` for `item_card.xml`. See `docs/GLOSSARY.md`.
- **Glide** — loads the item image. See `docs/GLOSSARY.md`.
- **Interface** — `OnItemClickListener` for card taps.

## Libraries and imports
- **AndroidX**: RecyclerView, LayoutInflater, Context, annotations.
- **Glide**: image loading with placeholder and cross-fade.
- **Project**: `Item` model, `ItemCardBinding`, type/status colors and strings.

## Fields
- `private final List<Item> items` — the list the Adapter owns (cleared/refilled by `updateItems`).
- `private final OnItemClickListener listener` — card-tap callback.

## Functions

### `public interface OnItemClickListener` — `void onItemClick(Item item)`
```java
public interface OnItemClickListener {
    void onItemClick(@NonNull Item item);
}
```
- **What it does:** Contract for “a card was tapped.”
- **Where it's called:** Implemented by lambdas in `SearchFragment` and `HomeFragment`; invoked from `ItemViewHolder.bind`.
- **Why it's written this way:** Same pattern as `PreviewItemAdapter` so list screens stay decoupled from the Adapter.
- **Step by step:**
  1. Host implements `onItemClick`.
  2. Adapter calls it with the tapped `Item`.

### `public ItemAdapter(List<Item> items, OnItemClickListener listener)`
```java
public ItemAdapter(@NonNull List<Item> items, @NonNull OnItemClickListener listener) {
    this.items = items;
    this.listener = listener;
}
```
- **What it does:** Keeps the caller’s list reference and the click listener.
- **Where it's called:** `SearchFragment` (empty `ArrayList` then updated); `HomeFragment` (per match group with that group’s matches).
- **Why it's written this way:** Taking the list in the constructor lets Home pass a subset without copying first; `updateItems` still clears and refills the same list object.
- **Step by step:**
  1. Store `items` and `listener`.

### `public void updateItems(List<Item> newItems)`
```java
public void updateItems(@NonNull List<Item> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
}
```
- **What it does:** Replaces contents of the held list and redraws.
- **Where it's called:** From `SearchFragment` when search results change.
- **Why it's written this way:** Mutates the same list instance the Adapter was given, then notifies.
- **Step by step:**
  1. Clear, addAll, `notifyDataSetChanged()`.

### `public ItemViewHolder onCreateViewHolder` / `onBindViewHolder` / `getItemCount`
```java
@NonNull
@Override
public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemCardBinding binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new ItemViewHolder(binding);
}

@Override
public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
    holder.bind(items.get(position));
}

@Override
public int getItemCount() {
    return items.size();
}
```
- **What it does:** Creates card ViewHolders, binds by position, reports count.
- **Where it's called:** By RecyclerView.
- **Why it's written this way:** Standard Adapter lifecycle methods.
- **Step by step:**
  1. Inflate `ItemCardBinding` into `ItemViewHolder`.
  2. Bind `items.get(position)`.
  3. Return `items.size()` for count.

### `ItemViewHolder.bind(Item item)`
```java
void bind(@NonNull Item item) {
    Context context = binding.getRoot().getContext();

    binding.textItemTitle.setText(item.getTitle());
    binding.textItemCategory.setText(item.getCategory());
    binding.textItemLocation.setText(item.getLocation());

    String type = item.getType();
    if ("Lost".equalsIgnoreCase(type)) {
        binding.chipType.setText(R.string.type_lost);
        binding.chipType.setChipBackgroundColorResource(R.color.type_lost);
    } else {
        binding.chipType.setText(type != null ? context.getString(R.string.type_found) : "");
        binding.chipType.setChipBackgroundColorResource(R.color.type_found);
    }
    binding.chipType.setTextColor(context.getColor(R.color.text_on_accent));

    String status = item.getStatus();
    if ("CLOSED".equalsIgnoreCase(status)) {
        binding.chipStatus.setText(R.string.status_closed);
        binding.chipStatus.setChipBackgroundColorResource(R.color.status_closed_bg);
        binding.chipStatus.setTextColor(context.getColor(R.color.status_closed_text));
    } else {
        binding.chipStatus.setText(R.string.status_open);
        binding.chipStatus.setChipBackgroundColorResource(R.color.status_open_bg);
        binding.chipStatus.setTextColor(context.getColor(R.color.status_open_text));
    }

    Glide.with(context)
            .load(item.getImageUrl())
            .placeholder(R.drawable.ic_item_placeholder)
            .error(R.drawable.ic_item_placeholder)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(binding.imageItem);

    binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
}
```
- **What it does:** Fills all card fields, styles type and status chips, loads the photo, and sets the root click listener.
- **Where it's called:** From `onBindViewHolder`.
- **Why it's written this way:** Search needs Open/Closed visibility that the smaller preview cards omit. Status defaults to Open styling when not `CLOSED`.
- **Step by step:**
  1. Set title, category, location.
  2. Style Lost vs Found type chip.
  3. Style Closed vs Open status chip.
  4. Load image with Glide.
  5. On root click, call `listener.onItemClick(item)`.
