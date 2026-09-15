```java
@Nullable
@Override
public View onCreateView(@NonNull LayoutInflater inflater,
                         @Nullable ViewGroup container,
                         @Nullable Bundle savedInstanceState) {
    binding = FragmentMyItemsBinding.inflate(inflater, container, false);
    return binding.getRoot();
}
```

What that function does: I inflate the my-items layout.
What the function is responsible for: I am the fragment view create.
Where it works: DashboardActivity and MyItemsActivity host me. I inflate fragment_my_items.xml.
Why I chose to write the code of the function the way its written: View Binding keeps ids typed.
More: I use FragmentMyItemsBinding.inflate.

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    apiService = RetrofitClient.getApiService();
    adapter = new MyReportAdapter(this);
    binding.recyclerMyItems.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.recyclerMyItems.setAdapter(adapter);

    binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_all_reports));
    binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.type_lost));
    binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.type_found));
    binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            currentTab = tab.getPosition();
            applyTabFilter();
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }
    });

    AppDataRefresh.observeNew(getViewLifecycleOwner(), this::loadMyItems);
}
```

What that function does: I set the list, the All/Lost/Found tabs, and a refresh observer.
What the function is responsible for: I am the fragment setup after inflate.
Where it works: Fragment calls me after onCreateView. I implement MyReportAdapter.Listener.
Why I chose to write the code of the function the way its written: Tabs filter locally so I do not refetch on each tab.
More: I use TabLayout and AppDataRefresh.observeNew.

```java
public void onTabSelected(TabLayout.Tab tab) {
    currentTab = tab.getPosition();
    applyTabFilter();
}
```

What that function does: I remember the tab and filter the list.
What the function is responsible for: I am the tab-selected callback.
Where it works: TabLayout calls me.
Why I chose to write the code of the function the way its written: Position 0/1/2 is All/Lost/Found.
More: I call applyTabFilter.

```java
public void onTabUnselected(TabLayout.Tab tab) {
}
```

What that function does: I do nothing when a tab is left.
What the function is responsible for: I fill the TabLayout listener.
Where it works: TabLayout calls me.
Why I chose to write the code of the function the way its written: Filter only needs onTabSelected.
More: I leave the body empty.

```java
public void onTabReselected(TabLayout.Tab tab) {
}
```

What that function does: I do nothing when the same tab is tapped again.
What the function is responsible for: I fill the TabLayout listener.
Where it works: TabLayout calls me.
Why I chose to write the code of the function the way its written: The list is already filtered.
More: I leave the body empty.

```java
@Override
public void onResume() {
    super.onResume();
    loadMyItems();
}
```

What that function does: I reload my reports whenever this tab comes back.
What the function is responsible for: I pick up edits made on ReportActivity.
Where it works: Android calls me. AppDataRefresh also calls loadMyItems.
Why I chose to write the code of the function the way its written: onResume covers returning from edit.
More: I call loadMyItems.

```java
private void loadMyItems() {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) {
        return;
    }

    binding.progress.setVisibility(View.VISIBLE);
    apiService.getMyItems(authHeader).enqueue(new Callback<List<Item>>() {
        @Override
        public void onResponse(@NonNull Call<List<Item>> call,
                               @NonNull Response<List<Item>> response) {
            if (binding == null || !isAdded()) {
                return;
            }
            binding.progress.setVisibility(View.GONE);
            if (response.isSuccessful() && response.body() != null) {
                allMyItems = response.body();
                allMyItems.sort(Comparator.comparing(Item::getReportedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                applyTabFilter();
            } else {
                ConnectionToasts.showIfOnline(requireContext(),
                        R.string.error_generic, Toast.LENGTH_SHORT);
            }
        }

        @Override
        public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
            if (binding == null || !isAdded()) {
                return;
            }
            binding.progress.setVisibility(View.GONE);
            ConnectionToasts.showIfOnline(requireContext(),
                    R.string.error_generic, Toast.LENGTH_SHORT);
        }
    });
}
```

What that function does: I fetch my reports, newest first, then I apply the tab filter.
What the function is responsible for: I am GET /api/items/my-items for this tab.
Where it works: I am called from onResume and AppDataRefresh.
Why I chose to write the code of the function the way its written: I check binding and isAdded so a late reply after leave does not crash.
More: I use Comparator.nullsLast reverseOrder on getReportedAt.

```java
public void onResponse(@NonNull Call<List<Item>> call,
                       @NonNull Response<List<Item>> response) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    if (response.isSuccessful() && response.body() != null) {
        allMyItems = response.body();
        allMyItems.sort(Comparator.comparing(Item::getReportedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        applyTabFilter();
    } else {
        ConnectionToasts.showIfOnline(requireContext(),
                R.string.error_generic, Toast.LENGTH_SHORT);
    }
}
```

What that function does: I store the list newest first and refresh the tab.
What the function is responsible for: I am the Retrofit success path for getMyItems.
Where it works: Retrofit calls me from loadMyItems.
Why I chose to write the code of the function the way its written: Sorting here keeps applyTabFilter simple.
More: I use Item::getReportedAt.

```java
public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    ConnectionToasts.showIfOnline(requireContext(),
            R.string.error_generic, Toast.LENGTH_SHORT);
}
```

What that function does: I hide the spinner and toast if the API is up.
What the function is responsible for: I am the Retrofit failure path for getMyItems.
Where it works: Retrofit calls me from loadMyItems.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline.

```java
private void applyTabFilter() {
    List<Item> filtered = new ArrayList<>();
    for (Item item : allMyItems) {
        if ("CLOSED".equalsIgnoreCase(item.getStatus())) {
            continue;
        }
        if (currentTab == 1 && !"LOST".equalsIgnoreCase(item.getType())) {
            continue;
        }
        if (currentTab == 2 && !"FOUND".equalsIgnoreCase(item.getType())) {
            continue;
        }
        filtered.add(item);
    }
    adapter.updateItems(filtered);
    boolean empty = filtered.isEmpty();
    binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    binding.recyclerMyItems.setVisibility(empty ? View.GONE : View.VISIBLE);
}
```

What that function does: I hide closed reports and keep All, Lost, or Found, then I show empty text if none.
What the function is responsible for: I am the local tab filter.
Where it works: I am called after load and when a tab is selected.
Why I chose to write the code of the function the way its written: Closed reports belong in history, not here.
More: I use Item.getStatus and getType.

```java
@Override
public void onEdit(@NonNull Item item) {
    Intent intent = new Intent(requireContext(), ReportActivity.class);
    intent.putExtra(ReportActivity.EXTRA_ITEM_ID, item.getId());
    startActivity(intent);
}
```

What that function does: I open the report form in edit mode for that item.
What the function is responsible for: I am the adapter edit callback.
Where it works: MyReportAdapter.bind calls me.
Why I chose to write the code of the function the way its written: EXTRA_ITEM_ID is how ReportActivity knows to load and PUT.
More: I use ReportActivity.EXTRA_ITEM_ID.

```java
@Override
public void onDelete(@NonNull Item item) {
    new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_report_title)
            .setMessage(getString(R.string.delete_report_message, item.getTitle()))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete, (d, w) -> deleteItem(item))
            .show();
}
```

What that function does: I ask before deleting a report.
What the function is responsible for: I am the adapter delete callback.
Where it works: MyReportAdapter.bind calls me.
Why I chose to write the code of the function the way its written: A confirm dialog stops an accidental delete.
More: I use MaterialAlertDialogBuilder.

```java
private void deleteItem(@NonNull Item item) {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null || item.getId() == null) {
        return;
    }

    binding.progress.setVisibility(View.VISIBLE);
    apiService.deleteItem(authHeader, item.getId()).enqueue(new Callback<Void>() {
        @Override
        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
            if (binding == null || !isAdded()) {
                return;
            }
            binding.progress.setVisibility(View.GONE);
            if (response.isSuccessful()) {
                Toast.makeText(requireContext(),
                        R.string.msg_report_deleted, Toast.LENGTH_SHORT).show();
                loadMyItems();
            } else {
                Toast.makeText(requireContext(),
                        R.string.error_generic, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
            if (binding == null || !isAdded()) {
                return;
            }
            binding.progress.setVisibility(View.GONE);
            Toast.makeText(requireContext(),
                    R.string.error_generic, Toast.LENGTH_SHORT).show();
        }
    });
}
```

What that function does: I DELETE the report, then I reload the list.
What the function is responsible for: I am DELETE /api/items/{id} from this tab.
Where it works: I am called from the confirm dialog in onDelete.
Why I chose to write the code of the function the way its written: Reloading is safer than removing one row if the server also closed related claims.
More: I use apiService.deleteItem.

```java
public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    if (response.isSuccessful()) {
        Toast.makeText(requireContext(),
                R.string.msg_report_deleted, Toast.LENGTH_SHORT).show();
        loadMyItems();
    } else {
        Toast.makeText(requireContext(),
                R.string.error_generic, Toast.LENGTH_SHORT).show();
    }
}
```

What that function does: I toast deleted and reload, or I toast a generic error.
What the function is responsible for: I am the Retrofit success path for deleteItem.
Where it works: Retrofit calls me from deleteItem.
Why I chose to write the code of the function the way its written: I check isAdded so a late reply is safe.
More: I call loadMyItems.

```java
public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    Toast.makeText(requireContext(),
            R.string.error_generic, Toast.LENGTH_SHORT).show();
}
```

What that function does: I hide the spinner and toast a generic error.
What the function is responsible for: I am the Retrofit failure path for deleteItem.
Where it works: Retrofit calls me from deleteItem.
Why I chose to write the code of the function the way its written: A generic toast is enough for delete.
More: I use error_generic.

```java
@Override
public void onDestroyView() {
    super.onDestroyView();
    binding = null;
}
```

What that function does: I drop the binding when the view is destroyed.
What the function is responsible for: I stop late callbacks from touching a dead view.
Where it works: Fragment calls me. loadMyItems checks binding == null.
Why I chose to write the code of the function the way its written: View Binding docs ask to null the binding here.
More: I set binding to null.
