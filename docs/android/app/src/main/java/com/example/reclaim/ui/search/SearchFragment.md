# SearchFragment.java

## Overview
This is the search **Fragment** (see `docs/GLOSSARY.md`). It shows open reports from a shared `ItemsViewModel` as a filterable list or a Google Map of markers. Users can search by text, filter by category/type, sort (including by distance if location permission is granted), and open item details.

## Concepts used here
- **Fragment** — search UI inside Dashboard or `SearchActivity`. See `docs/GLOSSARY.md`.
- **ViewModel / LiveData** — `ItemsViewModel` holds the item list and loading/error. See `docs/GLOSSARY.md`.
- **RecyclerView / Adapter** — `ItemAdapter` for list mode. See `docs/GLOSSARY.md`.
- **Google Maps** — markers for items with lat/lng. See `docs/GLOSSARY.md`.
- **Fused location** — last known location for nearer/farther sort.
- **Activity Result (permission)** — requests fine location when needed.

## Libraries and imports
- **AndroidX**: Fragment, ViewModelProvider, SwipeRefresh-related binding, PopupMenu, permission launcher.
- **Google Play services**: FusedLocationProviderClient, SupportMapFragment, GoogleMap, Marker, LatLng.
- **Project**: `ItemAdapter`, `ItemsViewModel`, `ItemFilterHelper`, `ItemNavigationHelper`, `AppDataRefresh`, `ConnectionToasts`.

## Fields
- `binding`, `viewModel`, `adapter` — UI, shared items ViewModel, list adapter.
- `googleMap`, `fusedLocationClient` — map and location client.
- `allItems`, `filteredItems` — raw vs filtered lists.
- `searchQuery`, `selectedCategory`, `typeFilter`, `dateSort`, `nameSort`, `locationSort` — filter/sort state.
- `userLatitude` / `userLongitude` — for location sort.
- `mapMode` — list vs map toggle.
- `markerItems` — map from Marker → Item for marker taps.
- `locationPermissionLauncher` — requests `ACCESS_FINE_LOCATION`.
- `DEFAULT_LOCATION` — Tel Aviv lat/lng used when no markers (32.0853, 34.7818).

## Functions

### `private void resetFilters()`
```java
private void resetFilters() {
    selectedCategory = null;
    typeFilter = ItemFilterHelper.TypeFilter.ALL;
    dateSort = ItemFilterHelper.DateSort.RECENT;
    nameSort = ItemFilterHelper.NameSort.NONE;
    locationSort = ItemFilterHelper.LocationSort.NONE;
}
```
- **What it does:** Restores default category/type/sort state.
- **Where it's called:** From `onFilterMenuItemSelected` for `menu_reset`.
- **Why it's written this way:** One place resets every filter field before `applyFilters`.
- **Step by step:**
  1. Clear category; type ALL; date RECENT; name/location sorts NONE.

### `private void ensureLocationForSort()` / `private void fetchUserLocation()`
```java
private void ensureLocationForSort() {
    if (/* fine location granted */) fetchUserLocation();
    else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
}

private void fetchUserLocation() {
    fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (location != null) {
                    userLatitude = location.getLatitude();
                    userLongitude = location.getLongitude();
                    applyFilters();
                }
            });
}
```
- **What it does:** Ensures permission then reads last location into lat/lng and re-applies filters.
- **Where it's called:** When the user picks nearer/farther sort; permission callback may call `fetchUserLocation` or clear location sort.
- **Why it's written this way:** Location sort needs coordinates; permission denial resets location sort and refilters.
- **Step by step:**
  1. If granted, `getLastLocation` and store coords → `applyFilters`.
  2. Else launch permission request.

### `private String buildFilterSummary()`
```java
private String buildFilterSummary() {
    // category + type + (if list mode) active sort label, joined by separator
}
```
- **What it does:** Builds the chip text summarizing active filters/sort.
- **Where it's called:** From `applyFilters` and `updateViewMode`.
- **Why it's written this way:** Map mode hides sort options in the menu, so the summary also omits sort labels in map mode.
- **Step by step:**
  1. Append category (or “all”) and type.
  2. If not map mode, append the active location/name/date sort label when relevant.

### `private void applyFilters()`
```java
private void applyFilters() {
    List<Item> filtered = ItemFilterHelper.apply(
            allItems, selectedCategory, searchQuery, typeFilter,
            dateSort, nameSort, locationSort, userLatitude, userLongitude);
    filteredItems = filtered;
    adapter.updateItems(filtered);
    renderMarkers(filtered);
    // empty view + filter chip
}
```
- **What it does:** Runs `ItemFilterHelper`, updates list and map markers, toggles empty state, refreshes the filter chip.
- **Where it's called:** Text changes, menu selections, ViewModel item updates, location fetch, map ready, permission denial path.
- **Why it's written this way:** One pipeline keeps list and map in sync.
- **Step by step:**
  1. Call `ItemFilterHelper.apply` with current state.
  2. Update adapter and markers.
  3. Show/hide empty and list visibility (list only when not map mode).
  4. Set chip text from `buildFilterSummary`.

### `private void updateViewMode()` / `private void renderMarkers(List<Item> items)`
```java
private void updateViewMode() {
    binding.swipeRefresh.setVisibility(mapMode ? View.GONE : View.VISIBLE);
    binding.mapContainer.setVisibility(mapMode ? View.VISIBLE : View.GONE);
    // refresh chip; if mapMode renderMarkers
}

private void renderMarkers(List<Item> items) {
    // clear map; add markers for items with lat/lng; camera to first or DEFAULT_LOCATION
}
```
- **What it does:** Swaps list vs map containers; draws markers and moves the camera.
- **Where it's called:** Toggle button → `updateViewMode`; `applyFilters` / `onMapReady` → `renderMarkers`.
- **Why it's written this way:** Items without coordinates are skipped. Default camera is Tel Aviv if none.
- **Step by step:**
  1. Toggle swipe-refresh vs map container visibility.
  2. Clear markers; for each item with coords add Marker and map it in `markerItems`.
  3. Move camera to first marker or `DEFAULT_LOCATION` at zoom 12.

### `private void showFilterMenu(View anchor)` / `private boolean onFilterMenuItemSelected(MenuItem item)`
```java
private void showFilterMenu(View anchor) {
    // PopupMenu from search_filter_menu; hide sort submenu in mapMode
}

private boolean onFilterMenuItemSelected(MenuItem item) {
    // set category / type / sorts; reset; applyFilters
}
```
- **What it does:** Shows the filter popup; applies the chosen category, type, or sort (requesting location for distance sorts).
- **Where it's called:** Filter button click; menu item clicks.
- **Why it's written this way:** Sort is hidden in map mode because map order is geographic, not list order. Choosing a date/name sort clears location sort and vice versa for competing sorts.
- **Step by step:**
  1. Inflate popup; hide `menu_sort` when `mapMode`.
  2. Map menu ids to filter fields; for location sorts call `ensureLocationForSort`.
  3. Reset path calls `resetFilters` then `applyFilters`.

### `public void onMapReady(GoogleMap map)`
```java
@Override
public void onMapReady(@NonNull GoogleMap map) {
    googleMap = map;
    googleMap.getUiSettings().setZoomControlsEnabled(true);
    // maybe setMyLocationEnabled; marker click → details; applyFilters()
}
```
- **What it does:** Stores the map, enables zoom (and my-location if permitted), opens details on marker click, then applies filters to draw markers.
- **Where it's called:** By Maps when the SupportMapFragment is ready (`getMapAsync(this)`).
- **Why it's written this way:** Marker → Item via `markerItems`; details Intent from `ItemNavigationHelper`.
- **Step by step:**
  1. Keep map reference; enable zoom controls.
  2. If location permission granted, enable my-location layer.
  3. On marker click, open details for the mapped Item.
  4. `applyFilters()`.

### `onCreateView` / `onViewCreated` / `onResume` / `onDestroyView`
```java
// inflate; wire adapter, swipe refresh, search text, view toggle, map async;
// observe ViewModel + AppDataRefresh; load on resume; null binding on destroy
```
- **What it does:** Builds the search UI and observers; reloads items on resume.
- **Where it's called:** Fragment lifecycle; hosted by `DashboardActivity` (search tab) or `SearchActivity`.
- **Why it's written this way:** Activity-scoped ViewModel shares items with Home. Swipe refresh calls `viewModel.loadItems()`.
- **Step by step:**
  1. Inflate binding; get ViewModel and location client.
  2. Wire list, search TextWatcher, filter button, list/map toggle, map async.
  3. Observe items/loading/error and AppDataRefresh.
  4. `onResume` → `loadItems`; `onDestroyView` → `binding = null`.
