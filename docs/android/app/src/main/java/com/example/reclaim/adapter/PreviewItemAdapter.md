```java
void onItemClick(@NonNull Item item);
```

What that function does: I am the card-tap callback the host must implement.
What the function is responsible for: I tell home which preview item was opened.
Where it works: ViewHolder.bind calls me. HomeFragment implements me.
Why I chose to write the code of the function the way its written: An interface keeps navigation out of the adapter.
More: I take a non-null Item.

```java
public PreviewItemAdapter(@NonNull OnItemClickListener listener) {
    this.listener = listener;
}
```

What that function does: I store the click listener.
What the function is responsible for: I am the adapter constructor.
Where it works: I am constructed in HomeFragment for the nearby preview row.
Why I chose to write the code of the function the way its written: I start with an empty list and fill it with updateItems.
More: I keep items as an ArrayList.

```java
public void updateItems(@NonNull List<Item> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
}
```

What that function does: I replace the list and redraw every small card.
What the function is responsible for: I am the bulk refresh for the home preview.
Where it works: I am called by HomeFragment.
Why I chose to write the code of the function the way its written: I clear and refill so the same adapter stays on the RecyclerView.
More: I use notifyDataSetChanged.

```java
@NonNull
@Override
public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemCardPreviewBinding binding = ItemCardPreviewBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
}
```

What that function does: I inflate item_card_preview for a new row.
What the function is responsible for: I am RecyclerView's create path.
Where it works: RecyclerView calls me. The layout is item_card_preview.xml.
Why I chose to write the code of the function the way its written: View Binding keeps ids typed.
More: I use ItemCardPreviewBinding.inflate.

```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(items.get(position));
}
```

What that function does: I bind one item to a small card.
What the function is responsible for: I am RecyclerView's bind path.
Where it works: RecyclerView calls me for each visible preview.
Why I chose to write the code of the function the way its written: The holder owns the field mapping.
More: I call ViewHolder.bind.

```java
@Override
public int getItemCount() {
    return items.size();
}
```

What that function does: I return how many preview items I hold.
What the function is responsible for: I tell RecyclerView the list size.
Where it works: RecyclerView calls me.
Why I chose to write the code of the function the way its written: Size matches the in-memory list.
More: I return items.size().

```java
ViewHolder(@NonNull ItemCardPreviewBinding binding) {
    super(binding.getRoot());
    this.binding = binding;
}
```

What that function does: I store the small-card binding.
What the function is responsible for: I am the row constructor.
Where it works: I am created in onCreateViewHolder.
Why I chose to write the code of the function the way its written: super(binding.getRoot()) is what RecyclerView needs.
More: I keep ItemCardPreviewBinding.

```java
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

What that function does: I fill title, place, type chip, photo, and the tap.
What the function is responsible for: I am one compact home preview card. I skip category and status.
Where it works: I am called from onBindViewHolder. Taps go to HomeFragment.
Why I chose to write the code of the function the way its written: The preview row is small, so I only show title, place, type, and photo.
More: I use Glide and DrawableTransitionOptions.withCrossFade(200).
