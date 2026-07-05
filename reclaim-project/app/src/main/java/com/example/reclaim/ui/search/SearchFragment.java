package com.example.reclaim.ui.search;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.reclaim.R;
import com.example.reclaim.adapter.ItemAdapter;
import com.example.reclaim.databinding.FragmentSearchBinding;
import com.example.reclaim.model.Item;
import com.example.reclaim.ui.dashboard.ItemFilterHelper;
import com.example.reclaim.ui.dashboard.ItemNavigationHelper;
import com.example.reclaim.ui.dashboard.ItemsViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Search & browse screen with list/map toggle and filter menu.
 */
public class SearchFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng DEFAULT_LOCATION = new LatLng(32.0853, 34.7818);

    private FragmentSearchBinding binding;
    private ItemsViewModel viewModel;
    private ItemAdapter adapter;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private List<Item> allItems = new ArrayList<>();
    private String searchQuery = "";
    private String selectedCategory = null;

    private ItemFilterHelper.TypeFilter typeFilter = ItemFilterHelper.TypeFilter.ALL;
    private ItemFilterHelper.DateSort dateSort = ItemFilterHelper.DateSort.RECENT;
    private ItemFilterHelper.NameSort nameSort = ItemFilterHelper.NameSort.NONE;
    private ItemFilterHelper.LocationSort locationSort = ItemFilterHelper.LocationSort.NONE;

    private Double userLatitude;
    private Double userLongitude;
    private boolean mapMode = false;
    private List<Item> filteredItems = new ArrayList<>();

    private final Map<Marker, Item> markerItems = new HashMap<>();

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            fetchUserLocation();
                        } else {
                            Toast.makeText(requireContext(),
                                    R.string.msg_location_permission_required,
                                    Toast.LENGTH_SHORT).show();
                            locationSort = ItemFilterHelper.LocationSort.NONE;
                            applyFilters();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString() : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.btnFilterSort.setOnClickListener(v -> showFilterMenu(v));
        binding.toggleViewMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            mapMode = checkedId == R.id.btn_view_map;
            updateViewMode();
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            allItems = items != null ? items : new ArrayList<>();
            applyFilters();
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null) {
                binding.swipeRefresh.setRefreshing(loading);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        if (viewModel.getItems().getValue() == null
                || viewModel.getItems().getValue().isEmpty()) {
            viewModel.loadItems();
        }
    }

    private void showFilterMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.search_filter_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this::onFilterMenuItemSelected);
        popup.show();
    }

    private boolean onFilterMenuItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.cat_all) {
            selectedCategory = null;
        } else if (id == R.id.cat_electronics) {
            selectedCategory = getString(R.string.filter_electronics);
        } else if (id == R.id.cat_documents) {
            selectedCategory = getString(R.string.filter_documents);
        } else if (id == R.id.cat_keys) {
            selectedCategory = getString(R.string.filter_keys);
        } else if (id == R.id.cat_wallets) {
            selectedCategory = getString(R.string.filter_wallets);
        } else if (id == R.id.cat_other) {
            selectedCategory = getString(R.string.filter_other);
        } else if (id == R.id.type_all) {
            typeFilter = ItemFilterHelper.TypeFilter.ALL;
        } else if (id == R.id.type_lost) {
            typeFilter = ItemFilterHelper.TypeFilter.LOST;
        } else if (id == R.id.type_found) {
            typeFilter = ItemFilterHelper.TypeFilter.FOUND;
        } else if (id == R.id.sort_date_recent) {
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
        } else {
            return false;
        }
        applyFilters();
        return true;
    }

    private void resetFilters() {
        selectedCategory = null;
        typeFilter = ItemFilterHelper.TypeFilter.ALL;
        dateSort = ItemFilterHelper.DateSort.RECENT;
        nameSort = ItemFilterHelper.NameSort.NONE;
        locationSort = ItemFilterHelper.LocationSort.NONE;
    }

    private void ensureLocationForSort() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

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
        if (!mapMode) {
            binding.recyclerItems.setVisibility(empty ? View.GONE : View.VISIBLE);
        }

        binding.chipActiveFilters.setVisibility(View.VISIBLE);
        binding.chipActiveFilters.setText(buildFilterSummary());
    }

    private String buildFilterSummary() {
        StringBuilder summary = new StringBuilder();
        if (selectedCategory != null) {
            summary.append(selectedCategory);
        } else {
            summary.append(getString(R.string.filter_all));
        }
        summary.append(" · ");
        if (typeFilter == ItemFilterHelper.TypeFilter.LOST) {
            summary.append(getString(R.string.type_lost));
        } else if (typeFilter == ItemFilterHelper.TypeFilter.FOUND) {
            summary.append(getString(R.string.type_found));
        } else {
            summary.append(getString(R.string.filter_type_all));
        }
        if (locationSort == ItemFilterHelper.LocationSort.NEARER) {
            summary.append(" · ").append(getString(R.string.sort_location_nearer));
        } else if (locationSort == ItemFilterHelper.LocationSort.FARTHER) {
            summary.append(" · ").append(getString(R.string.sort_location_farther));
        } else if (nameSort == ItemFilterHelper.NameSort.A_Z) {
            summary.append(" · ").append(getString(R.string.sort_name_az));
        } else if (nameSort == ItemFilterHelper.NameSort.Z_A) {
            summary.append(" · ").append(getString(R.string.sort_name_za));
        } else if (dateSort == ItemFilterHelper.DateSort.OLDEST) {
            summary.append(" · ").append(getString(R.string.sort_date_oldest));
        }
        return summary.toString();
    }

    private void updateViewMode() {
        binding.swipeRefresh.setVisibility(mapMode ? View.GONE : View.VISIBLE);
        binding.mapContainer.setVisibility(mapMode ? View.VISIBLE : View.GONE);
        if (mapMode && googleMap != null) {
            renderMarkers(filteredItems);
        }
    }

    private void renderMarkers(List<Item> items) {
        if (googleMap == null || items == null) {
            return;
        }
        googleMap.clear();
        markerItems.clear();

        LatLng firstPosition = null;
        for (Item item : items) {
            if (item.getLatitude() == null || item.getLongitude() == null) {
                continue;
            }
            LatLng position = new LatLng(item.getLatitude(), item.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(item.getTitle())
                    .snippet(item.getLocation()));
            if (marker != null) {
                markerItems.put(marker, item);
            }
            if (firstPosition == null) {
                firstPosition = position;
            }
        }

        if (firstPosition != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 12f));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 12f));
        }
    }

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
            if (item != null) {
                startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item));
            }
            return false;
        });
        applyFilters();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
