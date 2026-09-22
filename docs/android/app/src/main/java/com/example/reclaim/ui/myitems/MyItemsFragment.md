# MyItemsFragment.java

## Overview
This is the “My items” **Fragment** (see `docs/GLOSSARY.md`). It loads the signed-in user’s reports, filters out closed ones, and tabs between All / Lost / Found. Each row can edit (opens `ReportActivity` with the item id) or delete (confirm dialog + API).

## Concepts used here
- **Fragment** — list UI inside Dashboard or `MyItemsActivity`. See `docs/GLOSSARY.md`.
- **RecyclerView / Adapter** — `MyReportAdapter`. See `docs/GLOSSARY.md`.
- **TabLayout** — All / Lost / Found filters.
- **Retrofit Call / Callback** — `getMyItems`, `deleteItem`. See `docs/GLOSSARY.md`.
- **Intent** — opens `ReportActivity` for edit. See `docs/GLOSSARY.md`.
- **JWT (TokenManager)** — auth header. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX**: Fragment, LinearLayoutManager.
- **Material**: TabLayout, MaterialAlertDialogBuilder.
- **Retrofit**: Call, Callback, Response.
- **Project**: `MyReportAdapter`, `Item`, `ReportActivity`, `AppDataRefresh`, `ConnectionToasts`, API/token helpers.

## Fields
- `private FragmentMyItemsBinding binding` — my-items views.
- `private ReClaimApiService apiService` — API client.
- `private MyReportAdapter adapter` — report rows (this Fragment is the `Listener`).
- `private List<Item> allMyItems` — full list from the server (before tab filter).
- `private int currentTab` — 0 All, 1 Lost, 2 Found.

## Functions

### `private void applyTabFilter()`
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
- **What it does:** Builds the visible list: skips CLOSED items, optionally keeps only LOST or FOUND by tab, updates Adapter and empty state.
- **Where it's called:** After a successful `loadMyItems`, and when a tab is selected.
- **Why it's written this way:** Closed reports are history, not “my active items.” Type checks use `LOST`/`FOUND` as stored on the Item.
- **Step by step:**
  1. Loop `allMyItems`; skip CLOSED.
  2. If tab 1, require LOST; if tab 2, require FOUND.
  3. `adapter.updateItems`; toggle empty text vs RecyclerView.

### `private void loadMyItems()`
```java
private void loadMyItems() {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) {
        return;
    }
    binding.progress.setVisibility(View.VISIBLE);
    apiService.getMyItems(authHeader).enqueue(/* sort by reportedAt desc, applyTabFilter */);
}
```
- **What it does:** Fetches the user’s items, sorts newest first, then applies the current tab filter.
- **Where it's called:** From `onResume` and `AppDataRefresh.observeNew`.
- **Why it's written this way:** Sorting with nulls last keeps incomplete dates from breaking the order.
- **Step by step:**
  1. Require auth; show progress.
  2. On success: store body, sort by `getReportedAt` reverse, `applyTabFilter`.
  3. On error/failure: hide progress and `ConnectionToasts` / generic error.

### `private void deleteItem(Item item)`
```java
private void deleteItem(@NonNull Item item) {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null || item.getId() == null) {
        return;
    }
    binding.progress.setVisibility(View.VISIBLE);
    apiService.deleteItem(authHeader, item.getId()).enqueue(/* reload on success */);
}
```
- **What it does:** Deletes one report via the API and reloads the list on success.
- **Where it's called:** From `onDelete`’s confirm dialog positive button.
- **Why it's written this way:** Reloading after delete is simpler than removing one row locally.
- **Step by step:**
  1. Require auth and item id; show progress.
  2. Enqueue `deleteItem`.
  3. On success: toast deleted, `loadMyItems`; else toast error.

### `public void onEdit(Item item)` / `public void onDelete(Item item)`
```java
@Override
public void onEdit(@NonNull Item item) {
    Intent intent = new Intent(requireContext(), ReportActivity.class);
    intent.putExtra(ReportActivity.EXTRA_ITEM_ID, item.getId());
    startActivity(intent);
}

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
- **What it does:** Edit opens the report form with the item id; delete shows a confirm dialog then deletes.
- **Where it's called:** By `MyReportAdapter` button listeners (this Fragment implements `Listener`).
- **Why it's written this way:** Confirm before delete; edit reuses `ReportActivity`’s edit mode via extra.
- **Step by step:**
  1. `onEdit`: Intent to `ReportActivity` with `EXTRA_ITEM_ID`.
  2. `onDelete`: Material dialog; positive → `deleteItem`.

### `onCreateView` / `onViewCreated` / `onResume` / `onDestroyView`
```java
// inflate FragmentMyItemsBinding; wire RecyclerView + tabs; observe refresh; load on resume
```
- **What it does:** Sets up the list and three tabs; reloads on resume; clears binding on destroy.
- **Where it's called:** Fragment lifecycle; created by `DashboardActivity` (My Items tab) and `MyItemsActivity`.
- **Why it's written this way:** Tab selection only stores position and refilters; data load is separate.
- **Step by step:**
  1. Inflate binding; create Adapter with `this`.
  2. Add All/Lost/Found tabs; on select update `currentTab` and `applyTabFilter`.
  3. Observe `AppDataRefresh`; `onResume` → `loadMyItems`; `onDestroyView` nulls binding.
