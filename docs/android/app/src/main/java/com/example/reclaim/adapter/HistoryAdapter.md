```java
void onViewContact(@NonNull String claimId);
```

What that function does: I am the contact-button callback the host must implement.
What the function is responsible for: I tell the screen which approved claim to load contact for.
Where it works: HistoryViewHolder.bind calls me. ReportHistoryActivity implements me as showOwnerContact.
Why I chose to write the code of the function the way its written: The adapter only has claimId, not the full contact.
More: I take a non-null claimId.

```java
Status(@StringRes int labelRes, @ColorRes int bgColorRes, @ColorRes int textColorRes) {
    this.labelRes = labelRes;
    this.bgColorRes = bgColorRes;
    this.textColorRes = textColorRes;
}
```

What that function does: I store label and colors for a history badge.
What the function is responsible for: I am the enum constructor for PENDING, APPROVED, and DECLINED.
Where it works: Java uses me when the enum constants are created. bind reads the fields.
Why I chose to write the code of the function the way its written: Resources keep colors in one place instead of hex in Java.
More: I use @StringRes and @ColorRes.

```java
public Entry(String title, String imageUrl,
             @StringRes int roleLabelRes, Status status) {
    this(title, imageUrl, roleLabelRes, status, null, false);
}
```

What that function does: I build a row with no contact button.
What the function is responsible for: I am the short constructor for report rows.
Where it works: I am used in ReportHistoryActivity for the user's own reports.
Why I chose to write the code of the function the way its written: I forward to the full constructor with showContact false.
More: I pass null claimId.

```java
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

What that function does: I store one history row, including optional contact.
What the function is responsible for: I am the full row model.
Where it works: I am used in ReportHistoryActivity for claims, and by the short constructor.
Why I chose to write the code of the function the way its written: Contact stays optional so report rows do not show a button.
More: I keep title, imageUrl, roleLabelRes, status, claimId, and showContact.

```java
public HistoryAdapter(ContactClickListener contactClickListener) {
    this.contactClickListener = contactClickListener;
}
```

What that function does: I store the contact listener.
What the function is responsible for: I am the adapter constructor.
Where it works: I am constructed in ReportHistoryActivity.
Why I chose to write the code of the function the way its written: I start with an empty list and fill it with submit.
More: I keep entries as an ArrayList.

```java
public void submit(List<Entry> newEntries) {
    entries.clear();
    if (newEntries != null) {
        entries.addAll(newEntries);
    }
    notifyDataSetChanged();
}
```

What that function does: I replace the list and redraw every row.
What the function is responsible for: I am the bulk refresh after history loads.
Where it works: I am called by ReportHistoryActivity.
Why I chose to write the code of the function the way its written: A null list becomes empty.
More: I use notifyDataSetChanged.

```java
@NonNull
@Override
public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemHistoryBinding binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
    return new HistoryViewHolder(binding, contactClickListener);
}
```

What that function does: I inflate item_history for a new row.
What the function is responsible for: I am RecyclerView's create path.
Where it works: RecyclerView calls me. The layout is item_history.xml.
Why I chose to write the code of the function the way its written: I pass the listener into the holder so bind can use it.
More: I use ItemHistoryBinding.inflate.

```java
@Override
public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
    holder.bind(entries.get(position));
}
```

What that function does: I bind one entry to a row.
What the function is responsible for: I am RecyclerView's bind path.
Where it works: RecyclerView calls me for each visible row.
Why I chose to write the code of the function the way its written: The holder owns the field mapping.
More: I call HistoryViewHolder.bind.

```java
@Override
public int getItemCount() {
    return entries.size();
}
```

What that function does: I return how many history rows I hold.
What the function is responsible for: I tell RecyclerView the list size.
Where it works: RecyclerView calls me.
Why I chose to write the code of the function the way its written: Size matches the in-memory list.
More: I return entries.size().

```java
HistoryViewHolder(@NonNull ItemHistoryBinding binding,
                  ContactClickListener listener) {
    super(binding.getRoot());
    this.binding = binding;
    this.listener = listener;
}
```

What that function does: I store the row binding and the contact listener.
What the function is responsible for: I am the row constructor.
Where it works: I am created in onCreateViewHolder.
Why I chose to write the code of the function the way its written: super(binding.getRoot()) is what RecyclerView needs.
More: I keep ItemHistoryBinding and ContactClickListener.

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

What that function does: I fill role, title, photo, status badge, and the optional contact button.
What the function is responsible for: I am one history row.
Where it works: I am called from onBindViewHolder. Contact taps go to ReportHistoryActivity.
Why I chose to write the code of the function the way its written: I hide the button unless showContact and claimId are set. Status colors come from the enum.
More: I use Glide, ColorStateList, ContextCompat.getColor, and TextUtils.
