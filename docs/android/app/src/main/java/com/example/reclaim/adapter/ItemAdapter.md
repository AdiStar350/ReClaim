```java
void onItemClick(@NonNull Item item);
```

What that function does: I am the card-tap callback the host must implement.
What the function is responsible for: I tell the screen which item was opened.
Where it works: ItemViewHolder.bind calls me. SearchFragment and HomeFragment implement me.
Why I chose to write the code of the function the way its written: An interface keeps navigation out of the adapter.
More: I take a non-null Item.

```java
public ItemAdapter(@NonNull List<Item> items, @NonNull OnItemClickListener listener) {
    this.items = items;
    this.listener = listener;
}
```

What that function does: I store the list and the click listener.
What the function is responsible for: I am the adapter constructor.
Where it works: I am constructed in SearchFragment and HomeFragment.
Why I chose to write the code of the function the way its written: I keep the caller's list so updateItems can clear and refill the same instance.
More: I store List<Item> and OnItemClickListener.

```java
@NonNull
@Override
public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemCardBinding binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new ItemViewHolder(binding);
}
```

What that function does: I inflate item_card for a new row.
What the function is responsible for: I am RecyclerView's create path.
Where it works: RecyclerView calls me. The layout is item_card.xml.
Why I chose to write the code of the function the way its written: View Binding keeps ids typed.
More: I use ItemCardBinding.inflate.

```java
@Override
public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
    holder.bind(items.get(position));
}
```

What that function does: I bind one item to a card.
What the function is responsible for: I am RecyclerView's bind path.
Where it works: RecyclerView calls me for each visible card.
Why I chose to write the code of the function the way its written: The holder owns the field mapping.
More: I call ItemViewHolder.bind.

```java
@Override
public int getItemCount() {
    return items.size();
}
```

What that function does: I return how many items I hold.
What the function is responsible for: I tell RecyclerView the list size.
Where it works: RecyclerView calls me.
Why I chose to write the code of the function the way its written: Size matches the in-memory list.
More: I return items.size().

```java
public void updateItems(@NonNull List<Item> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
}
```

What that function does: I replace the list and redraw every card.
What the function is responsible for: I am the bulk refresh after a search or match load.
Where it works: I am called by SearchFragment and HomeFragment.
Why I chose to write the code of the function the way its written: I clear the same list instance the constructor stored.
More: I use notifyDataSetChanged.

```java
ItemViewHolder(@NonNull ItemCardBinding binding) {
    super(binding.getRoot());
    this.binding = binding;
}
```

What that function does: I store the card binding.
What the function is responsible for: I am the row constructor.
Where it works: I am created in onCreateViewHolder.
Why I chose to write the code of the function the way its written: super(binding.getRoot()) is what RecyclerView needs.
More: I keep ItemCardBinding.

```java
void bind(@NonNull Item item) {
    Context context = binding.getRoot().getContext();

    // Title, category, place
    binding.textItemTitle.setText(item.getTitle());
    binding.textItemCategory.setText(item.getCategory());
    binding.textItemLocation.setText(item.getLocation());

    // Lost / Found chip
    String type = item.getType();
    if ("Lost".equalsIgnoreCase(type)) {
        binding.chipType.setText(R.string.type_lost);
        // Dark red, white text
        binding.chipType.setChipBackgroundColorResource(R.color.type_lost);
    } else {
        binding.chipType.setText(type != null ? context.getString(R.string.type_found) : "");
        // Dark green, white text
        binding.chipType.setChipBackgroundColorResource(R.color.type_found);
    }
    binding.chipType.setTextColor(context.getColor(R.color.text_on_accent));

    // Open / Closed chip
    String status = item.getStatus();
    if ("CLOSED".equalsIgnoreCase(status)) {
        // Slate, cream text
        binding.chipStatus.setText(R.string.status_closed);
        binding.chipStatus.setChipBackgroundColorResource(R.color.status_closed_bg);
        binding.chipStatus.setTextColor(context.getColor(R.color.status_closed_text));
    } else {
        // Light green, dark text
        binding.chipStatus.setText(R.string.status_open);
        binding.chipStatus.setChipBackgroundColorResource(R.color.status_open_bg);
        binding.chipStatus.setTextColor(context.getColor(R.color.status_open_text));
    }

    // Photo
    Glide.with(context)
            .load(item.getImageUrl())
            .placeholder(R.drawable.ic_item_placeholder)
            .error(R.drawable.ic_item_placeholder)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(binding.imageItem);

    // Tap
    binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
}
```

What that function does: I fill title, category, place, type chip, status chip, photo, and the tap.
What the function is responsible for: I am one full item card.
Where it works: I am called from onBindViewHolder. Taps go to SearchFragment or HomeFragment.
Why I chose to write the code of the function the way its written: Lost/Found and Open/Closed use different chip colors so the list is easy to scan. A 200ms fade hides image pop-in.
More: I use Glide, DrawableTransitionOptions.withCrossFade, and chip color resources.
