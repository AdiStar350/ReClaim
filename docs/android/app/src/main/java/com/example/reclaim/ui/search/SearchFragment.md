```java
@Nullable
@Override
public View onCreateView(@NonNull LayoutInflater inflater,
                         @Nullable ViewGroup container,
                         @Nullable Bundle savedInstanceState) {
    binding = FragmentSearchBinding.inflate(inflater, container, false);
    return binding.getRoot();
}
```

What that function does: I inflate the search layout.
What the function is responsible for: I am the fragment view create.
Where it works: DashboardActivity and SearchActivity host me. I inflate fragment_search.xml.
Why I chose to write the code of the function the way its written: View Binding keeps ids typed.
More: I use FragmentSearchBinding.inflate.

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(requireActivity()).get(ItemsViewModel.class);
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

    adapter = new ItemAdapter(new ArrayList<>(), item ->
            startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item)));

    binding.recyclerItems.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.recyclerItems.setAdapter(adapter);

    binding.swipeRefresh.setColorSchemeResources(R.color.brand_accent, R.color.brand_slate);
    binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadItems());

    binding.editSearch.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            searchQuery = s != null ? s.toString() : "";
            applyFilters();
        }

        @Override
        public void afterTextChanged(Editable s) { }
    });

    binding.btnFilterSort.setOnClickListener(v -> showFilterMenu(v));
    binding.toggleViewMode.addOnButtonCheckedListener(
            (group, checkedId, isChecked) -> {
        if (!isChecked) return;

        mapMode = checkedId == R.id.btn_view_map;
        updateViewMode();
    });

    SupportMapFragment mapFragment = (SupportMapFragment)
            getChildFragmentManager().findFragmentById(R.id.map);

    if (mapFragment != null) mapFragment.getMapAsync(this);

    viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
        allItems = items != null ? items : new ArrayList<>();
        applyFilters();
    });

    viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
        if (loading != null) binding.swipeRefresh.setRefreshing(loading);
    });

    viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
        if (message != null && !message.isEmpty())
            ConnectionToasts.showIfOnline(requireContext(), message, Toast.LENGTH_SHORT);
    });

    AppDataRefresh.observeNew(getViewLifecycleOwner(), () -> viewModel.loadItems());
}
```

What that function does: I wire the list, search box, filters, map, and the shared ItemsViewModel.
What the function is responsible for: I am search setup.
Where it works: Fragment calls me after onCreateView. Cards open ItemDetailsActivity.
Why I chose to write the code of the function the way its written: Sharing ItemsViewModel with Home avoids a second fetch. TextWatcher filters as the user types.
More: I use FusedLocationProviderClient, SupportMapFragment, and ItemFilterHelper later.

```java
public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
```

What that function does: I do nothing before the text changes.
What the function is responsible for: I fill the TextWatcher.
Where it works: editSearch calls me.
Why I chose to write the code of the function the way its written: I only need onTextChanged.
More: I leave the body empty.

```java
public void onTextChanged(CharSequence s, int start, int before, int count) {
    searchQuery = s != null ? s.toString() : "";
    applyFilters();
}
```

What that function does: I save the typed query and refilter.
What the function is responsible for: I am live search.
Where it works: editSearch calls me.
Why I chose to write the code of the function the way its written: Filtering locally is fast enough on the ViewModel list.
More: I call applyFilters.

```java
public void afterTextChanged(Editable s) { }
```

What that function does: I do nothing after the text changes.
What the function is responsible for: I fill the TextWatcher.
Where it works: editSearch calls me.
Why I chose to write the code of the function the way its written: onTextChanged already filtered.
More: I leave the body empty.

```java
@Override
public void onResume() {
    super.onResume();
    viewModel.loadItems();
}
```

What that function does: I reload items when this tab comes back.
What the function is responsible for: I keep the list fresh.
Where it works: Android calls me. AppDataRefresh also loads.
Why I chose to write the code of the function the way its written: onResume covers returning from details.
More: I call viewModel.loadItems.

```java
private void showFilterMenu(View anchor) {
    ContextThemeWrapper wrapper = new ContextThemeWrapper(
            requireContext(), R.style.ThemeOverlay_ReClaim_PopupMenu);
    PopupMenu popup = new PopupMenu(wrapper, anchor);
    popup.getMenuInflater().inflate(R.menu.search_filter_menu, popup.getMenu());
    MenuItem sortMenu = popup.getMenu().findItem(R.id.menu_sort);
    if (sortMenu != null) {
        sortMenu.setVisible(!mapMode);
    }
    popup.setOnMenuItemClickListener(this::onFilterMenuItemSelected);
    popup.show();
}
```

What that function does: I show the filter/sort popup. Sort is hidden in map mode.
What the function is responsible for: I am the filter button menu.
Where it works: I am called from btnFilterSort.
Why I chose to write the code of the function the way its written: Map mode does not use name/date sort, so I hide that submenu.
More: I use PopupMenu and search_filter_menu.

```java
private boolean onFilterMenuItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.cat_all) selectedCategory = null;
    else if (id == R.id.cat_electronics) selectedCategory = getString(R.string.filter_electronics);
    else if (id == R.id.cat_documents) selectedCategory = getString(R.string.filter_documents);
    else if (id == R.id.cat_keys) selectedCategory = getString(R.string.filter_keys);
    else if (id == R.id.cat_wallets) selectedCategory = getString(R.string.filter_wallets);
    else if (id == R.id.cat_other) selectedCategory = getString(R.string.filter_other);
    else if (id == R.id.type_all) typeFilter = ItemFilterHelper.TypeFilter.ALL;
    else if (id == R.id.type_lost) typeFilter = ItemFilterHelper.TypeFilter.LOST;
    else if (id == R.id.type_found) typeFilter = ItemFilterHelper.TypeFilter.FOUND;
    else if (id == R.id.sort_date_recent) {
        dateSort = ItemFilterHelper.DateSort.RECENT;
        nameSort = ItemFilterHelper.NameSort.NONE;
        locationSort = ItemFilterHelper.LocationSort.NONE;
    } else if (id == R.id.sort_date_oldest) {
        dateSort = ItemFilterHelper.DateSort.OLDEST;
        nameSort = ItemFilterHelper.NameSort.NONE;
        locationSort = ItemFilterHelper.LocationSort.NONE;
    } else if (id == R.id.sort_name_az) {
        nameSort = ItemFilterHelper.NameSort.A_Z;
        locationSort = ItemFilterHelper.LocationSort.NONE;
    } else if (id == R.id.sort_name_za) {
        nameSort = ItemFilterHelper.NameSort.Z_A;
        locationSort = ItemFilterHelper.LocationSort.NONE;
    } else if (id == R.id.sort_location_nearer) {
        locationSort = ItemFilterHelper.LocationSort.NEARER;
        ensureLocationForSort();
    } else if (id == R.id.sort_location_farther) {
        locationSort = ItemFilterHelper.LocationSort.FARTHER;
        ensureLocationForSort();
    } else if (id == R.id.menu_reset) {
        resetFilters();
        applyFilters();
        return true;
    } else return false;

    applyFilters();
    return true;
}
```

What that function does: I set category, type, or sort from the menu, then I refilter.
What the function is responsible for: I am the popup click handler.
Where it works: PopupMenu calls me.
Why I chose to write the code of the function the way its written: One sort family at a time. Location sort asks for permission first.
More: I use ItemFilterHelper enums and ensureLocationForSort.

```java
private void resetFilters() {
    selectedCategory = null;
    typeFilter = ItemFilterHelper.TypeFilter.ALL;
    dateSort = ItemFilterHelper.DateSort.RECENT;
    nameSort = ItemFilterHelper.NameSort.NONE;
    locationSort = ItemFilterHelper.LocationSort.NONE;
}
```

What that function does: I put filters back to All / recent.
What the function is responsible for: I am the reset menu action.
Where it works: I am called from onFilterMenuItemSelected.
Why I chose to write the code of the function the way its written: Recent date is the default people expect.
More: I clear selectedCategory.

```java
private void ensureLocationForSort() {
    if (ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
        fetchUserLocation();
    } else {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }
}
```

What that function does: I fetch location if allowed, or I ask for permission.
What the function is responsible for: I gate nearer/farther sort.
Where it works: I am called when the user picks a location sort.
Why I chose to write the code of the function the way its written: I do not fetch without ACCESS_FINE_LOCATION.
More: I use locationPermissionLauncher.

```java
private void fetchUserLocation() {
    if (ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
        return;
    }

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

What that function does: I read last known location and refilter.
What the function is responsible for: I fill userLatitude/userLongitude for distance sort.
Where it works: I am called from ensureLocationForSort and the permission launcher.
Why I chose to write the code of the function the way its written: Last location is enough for sort. A null location leaves distance as MAX_VALUE in the helper.
More: I use FusedLocationProviderClient.getLastLocation.

```java
private void applyFilters() {
    List<Item> filtered = ItemFilterHelper.apply(
            allItems,
            selectedCategory,
            searchQuery,
            typeFilter,
            dateSort,
            nameSort,
            locationSort,
            userLatitude,
            userLongitude);

    filteredItems = filtered;
    adapter.updateItems(filtered);
    renderMarkers(filtered);

    boolean empty = filtered.isEmpty();
    binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

    if (!mapMode)
        binding.recyclerItems.setVisibility(empty ? View.GONE : View.VISIBLE);

    binding.chipActiveFilters.setVisibility(View.VISIBLE);
    binding.chipActiveFilters.setText(buildFilterSummary());
}
```

What that function does: I filter, update the list and map, and show the summary chip.
What the function is responsible for: I am the local search apply.
Where it works: I am called after typing, menu, location, and item loads.
Why I chose to write the code of the function the way its written: One method keeps list and markers in sync.
More: I use ItemFilterHelper.apply.

```java
private String buildFilterSummary() {
    StringBuilder summary = new StringBuilder();

    String separator = getString(R.string.label_filter_separator);

    if (selectedCategory != null) summary.append(selectedCategory);
    else summary.append(getString(R.string.filter_all));

    summary.append(separator);

    if (typeFilter == ItemFilterHelper.TypeFilter.LOST) summary.append(getString(R.string.type_lost));
    else if (typeFilter == ItemFilterHelper.TypeFilter.FOUND) summary.append(getString(R.string.type_found));
    else summary.append(getString(R.string.filter_type_all));

    if (!mapMode) {
        if (locationSort == ItemFilterHelper.LocationSort.NEARER)
            summary.append(separator).append(getString(R.string.sort_location_nearer));
        else if (locationSort == ItemFilterHelper.LocationSort.FARTHER)
            summary.append(separator).append(getString(R.string.sort_location_farther));
        else if (nameSort == ItemFilterHelper.NameSort.A_Z)
            summary.append(separator).append(getString(R.string.sort_name_az));
        else if (nameSort == ItemFilterHelper.NameSort.Z_A)
            summary.append(separator).append(getString(R.string.sort_name_za));
        else if (dateSort == ItemFilterHelper.DateSort.OLDEST)
            summary.append(separator).append(getString(R.string.sort_date_oldest));
    }

    return summary.toString();
}
```

What that function does: I build a short chip label for the active filters.
What the function is responsible for: I show what is on besides the default.
Where it works: applyFilters and updateViewMode set chipActiveFilters from me.
Why I chose to write the code of the function the way its written: Map mode skips sort text because sort is hidden. Recent date is the default so I omit it.
More: I use string resources for each filter name.

```java
private void updateViewMode() {
    binding.swipeRefresh.setVisibility(mapMode ? View.GONE : View.VISIBLE);
    binding.mapContainer.setVisibility(mapMode ? View.VISIBLE : View.GONE);
    binding.chipActiveFilters.setText(buildFilterSummary());

    if (mapMode && googleMap != null) renderMarkers(filteredItems);
}
```

What that function does: I switch between the list and the map.
What the function is responsible for: I am the list/map toggle.
Where it works: The toggle button listener calls me.
Why I chose to write the code of the function the way its written: I hide swipe refresh in map mode so pull-to-refresh does not fight the map.
More: I call renderMarkers when entering map mode.

```java
private void renderMarkers(List<Item> items) {
    if (googleMap == null || items == null) return;

    googleMap.clear();
    markerItems.clear();

    LatLng firstPosition = null;

    for (Item item : items) {
        if (item.getLatitude() == null || item.getLongitude() == null)
            continue;

        LatLng position = new LatLng(item.getLatitude(), item.getLongitude());
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(position)
                .title(item.getTitle())
                .snippet(item.getLocation()));

        if (marker != null) markerItems.put(marker, item);
        if (firstPosition == null) firstPosition = position;
    }

    if (firstPosition != null)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 12f));
    else
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 12f));
}
```

What that function does: I drop pins for filtered items and move the camera.
What the function is responsible for: I am the map draw.
Where it works: I am called from applyFilters, updateViewMode, and onMapReady.
Why I chose to write the code of the function the way its written: Items without a pin are skipped. DEFAULT_LOCATION is Tel Aviv when none have coords.
More: I use MarkerOptions and a HashMap from Marker to Item.

```java
@Override
public void onMapReady(@NonNull GoogleMap map) {
    googleMap = map;
    googleMap.getUiSettings().setZoomControlsEnabled(true);

    if (ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
        googleMap.setMyLocationEnabled(true);
    }

    googleMap.setOnMarkerClickListener(marker -> {
        Item item = markerItems.get(marker);

        if (item != null)
            startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item));

        return false;
    });

    applyFilters();
}
```

What that function does: I keep the map, enable zoom, and open details when a pin is tapped.
What the function is responsible for: I am OnMapReadyCallback.
Where it works: SupportMapFragment.getMapAsync calls me.
Why I chose to write the code of the function the way its written: My location is only on if permission is already granted. A pin tap uses the same details Intent as a card tap.
More: I use GoogleMap.setOnMarkerClickListener and ItemNavigationHelper.

```java
@Override
public void onDestroyView() {
    super.onDestroyView();
    binding = null;
}
```

What that function does: I drop the binding when the view is destroyed.
What the function is responsible for: I stop observers from touching a dead view.
Where it works: Fragment calls me.
Why I chose to write the code of the function the way its written: View Binding docs ask to null the binding here.
More: I set binding to null.
