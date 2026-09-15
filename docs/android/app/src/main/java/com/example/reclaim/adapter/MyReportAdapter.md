```java
void onEdit(@NonNull Item item);
```

What that function does: I am the edit-button callback the host must implement.
What the function is responsible for: I tell the screen which report to edit.
Where it works: ViewHolder.bind calls me. MyItemsFragment implements Listener.
Why I chose to write the code of the function the way its written: Edit stays a button, not a card tap.
More: I take a non-null Item.

```java
void onDelete(@NonNull Item item);
```

What that function does: I am the delete-button callback the host must implement.
What the function is responsible for: I tell the screen which report to delete.
Where it works: ViewHolder.bind calls me. MyItemsFragment implements Listener.
Why I chose to write the code of the function the way its written: Delete stays a button so a stray tap does not remove a report.
More: I take a non-null Item.

```java
public MyReportAdapter(@NonNull Listener listener) {
    this.listener = listener;
}
```

What that function does: I store the edit/delete listener.
What the function is responsible for: I am the adapter constructor.
Where it works: I am constructed in MyItemsFragment.
Why I chose to write the code of the function the way its written: I start with an empty list and fill it with updateItems.
More: I keep items as an ArrayList.

```java
public void updateItems(@NonNull List<Item> newItems) {
    items.clear();
    items.addAll(newItems);
    notifyDataSetChanged();
}
```

What that function does: I replace the list and redraw every row.
What the function is responsible for: I am the bulk refresh after my-items loads.
Where it works: I am called by MyItemsFragment.
Why I chose to write the code of the function the way its written: I clear and refill so the same adapter stays on the RecyclerView.
More: I use notifyDataSetChanged.

```java
@NonNull
@Override
public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemMyReportBinding binding = ItemMyReportBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
}
```

What that function does: I inflate item_my_report for a new row.
What the function is responsible for: I am RecyclerView's create path.
Where it works: RecyclerView calls me. The layout is item_my_report.xml.
Why I chose to write the code of the function the way its written: View Binding keeps ids typed.
More: I use ItemMyReportBinding.inflate.

```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(items.get(position));
}
```

What that function does: I bind one report to a row.
What the function is responsible for: I am RecyclerView's bind path.
Where it works: RecyclerView calls me for each visible row.
Why I chose to write the code of the function the way its written: The holder owns the field mapping.
More: I call ViewHolder.bind.

```java
@Override
public int getItemCount() {
    return items.size();
}
```

What that function does: I return how many of my reports I hold.
What the function is responsible for: I tell RecyclerView the list size.
Where it works: RecyclerView calls me.
Why I chose to write the code of the function the way its written: Size matches the in-memory list.
More: I return items.size().

```java
ViewHolder(@NonNull ItemMyReportBinding binding) {
    super(binding.getRoot());
    this.binding = binding;
}
```

What that function does: I store the row binding.
What the function is responsible for: I am the row constructor.
Where it works: I am created in onCreateViewHolder.
Why I chose to write the code of the function the way its written: super(binding.getRoot()) is what RecyclerView needs.
More: I keep ItemMyReportBinding.

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
    // White text on the dark chip
    binding.chipType.setTextColor(context.getColor(R.color.text_on_accent));

    // Only the buttons do something. The card
    // itself is not clickable, so a stray tap
    // does not open the edit form.
    binding.btnEdit.setOnClickListener(v -> listener.onEdit(item));
    binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    binding.getRoot().setOnClickListener(null);
    binding.getRoot().setClickable(false);
}
```

What that function does: I fill title, category, place, photo, type chip, and the edit/delete buttons.
What the function is responsible for: I am one of the user's own report rows.
Where it works: I am called from onBindViewHolder. Buttons go to MyItemsFragment.
Why I chose to write the code of the function the way its written: I turn off the card click so a stray tap does not open edit.
More: I use Glide, DrawableTransitionOptions.withCrossFade(200), and Listener.onEdit/onDelete.
