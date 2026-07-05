package com.example.reclaim.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.reclaim.databinding.FragmentListBinding;
import com.example.reclaim.model.Item;
import com.example.reclaim.model.User;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dashboard home screen with user greeting, stats, and advanced item filters.
 */
public class ListFragment extends Fragment {

    private FragmentListBinding binding;
    private ItemsViewModel viewModel;
    private ItemAdapter adapter;
    private ReClaimApiService apiService;
    private FusedLocationProviderClient fusedLocationClient;

    private List<Item> allItems = new ArrayList<>();
    private String selectedCategory = null;
    private String searchQuery = "";

    private ItemFilterHelper.TypeFilter typeFilter = ItemFilterHelper.TypeFilter.ALL;
    private ItemFilterHelper.DateSort dateSort = ItemFilterHelper.DateSort.RECENT;
    private ItemFilterHelper.NameSort nameSort = ItemFilterHelper.NameSort.NONE;
    private ItemFilterHelper.LocationSort locationSort = ItemFilterHelper.LocationSort.NONE;
    private boolean dateSortActive = true;
    private boolean locationSortActive = false;

    private Double userLatitude;
    private Double userLongitude;

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
                            locationSortActive = false;
                            locationSort = ItemFilterHelper.LocationSort.NONE;
                            updateSortChipStyles();
                            applyFilters();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ItemsViewModel.class);
        apiService = RetrofitClient.getApiService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        setupRecyclerView();
        setupChipGroup();
        setupSortChips();
        setupSearch();
        setupSwipeRefresh();
        loadUserGreeting();
        observeViewModel();

        if (viewModel.getItems().getValue() == null
                || viewModel.getItems().getValue().isEmpty()) {
            viewModel.loadItems();
        }
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter(new ArrayList<>(), item ->
                startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item)));
        binding.recyclerItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerItems.setAdapter(adapter);
    }

    private void setupChipGroup() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = null;
            } else {
                Chip selectedChip = group.findViewById(checkedIds.get(0));
                if (selectedChip != null && selectedChip.getId() == binding.chipAll.getId()) {
                    selectedCategory = null;
                } else if (selectedChip != null) {
                    selectedCategory = selectedChip.getText().toString();
                }
            }
            applyFilters();
        });

        binding.chipGroupType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                typeFilter = ItemFilterHelper.TypeFilter.ALL;
            } else {
                int id = checkedIds.get(0);
                if (id == binding.chipTypeLost.getId()) {
                    typeFilter = ItemFilterHelper.TypeFilter.LOST;
                } else if (id == binding.chipTypeFound.getId()) {
                    typeFilter = ItemFilterHelper.TypeFilter.FOUND;
                } else {
                    typeFilter = ItemFilterHelper.TypeFilter.ALL;
                }
            }
            applyFilters();
        });
    }

    private void setupSortChips() {
        updateSortChipStyles();

        binding.chipSortLocation.setOnClickListener(v -> {
            if (!locationSortActive) {
                locationSortActive = true;
                locationSort = ItemFilterHelper.LocationSort.NEARER;
                ensureLocationForSort();
            } else if (locationSort == ItemFilterHelper.LocationSort.NEARER) {
                locationSort = ItemFilterHelper.LocationSort.FARTHER;
                applyFilters();
            } else {
                locationSort = ItemFilterHelper.LocationSort.NEARER;
                applyFilters();
            }
            updateSortChipStyles();
        });

        binding.chipSortDate.setOnClickListener(v -> {
            dateSortActive = true;
            if (dateSort == ItemFilterHelper.DateSort.RECENT) {
                dateSort = ItemFilterHelper.DateSort.OLDEST;
            } else {
                dateSort = ItemFilterHelper.DateSort.RECENT;
            }
            updateSortChipStyles();
            applyFilters();
        });

        binding.chipSortName.setOnClickListener(v -> {
            if (nameSort == ItemFilterHelper.NameSort.NONE
                    || nameSort == ItemFilterHelper.NameSort.Z_A) {
                nameSort = ItemFilterHelper.NameSort.A_Z;
            } else {
                nameSort = ItemFilterHelper.NameSort.Z_A;
            }
            updateSortChipStyles();
            applyFilters();
        });
    }

    private void updateSortChipStyles() {
        styleSortChip(binding.chipSortLocation, locationSortActive);
        styleSortChip(binding.chipSortDate, dateSortActive);
        styleSortChip(binding.chipSortName, nameSort != ItemFilterHelper.NameSort.NONE);

        binding.chipSortLocation.setText(
                locationSort == ItemFilterHelper.LocationSort.FARTHER
                        ? R.string.sort_location_farther
                        : R.string.sort_location_nearer);
        binding.chipSortDate.setText(
                dateSort == ItemFilterHelper.DateSort.OLDEST
                        ? R.string.sort_date_oldest
                        : R.string.sort_date_recent);
        binding.chipSortName.setText(
                nameSort == ItemFilterHelper.NameSort.Z_A
                        ? R.string.sort_name_za
                        : R.string.sort_name_az);
    }

    private void styleSortChip(Chip chip, boolean active) {
        int bg = active ? R.color.chip_filter_selected : R.color.md_theme_surfaceContainerHigh;
        int text = active ? R.color.brand_cream : R.color.brand_dark;
        chip.setChipBackgroundColorResource(bg);
        chip.setTextColor(requireContext().getColor(text));
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
                    } else {
                        Toast.makeText(requireContext(),
                                R.string.msg_location_not_available,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupSearch() {
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
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
                R.color.brand_accent,
                R.color.brand_slate);
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadItems();
            loadUserGreeting();
        });
    }

    private void loadUserGreeting() {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) {
            return;
        }

        apiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (binding == null || !isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    String name = response.body().getName();
                    if (name != null && !name.isEmpty()) {
                        binding.textGreeting.setText(
                                getString(R.string.dashboard_greeting_named, name));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                // Keep default greeting
            }
        });
    }

    private void observeViewModel() {
        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            allItems = items != null ? items : new ArrayList<>();
            updateStats(allItems);
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
    }

    private void updateStats(List<Item> items) {
        int lost = 0;
        int found = 0;
        for (Item item : items) {
            if ("LOST".equalsIgnoreCase(item.getType())) {
                lost++;
            } else if ("FOUND".equalsIgnoreCase(item.getType())) {
                found++;
            }
        }
        binding.textStatTotal.setText(String.valueOf(items.size()));
        binding.textStatLost.setText(String.valueOf(lost));
        binding.textStatFound.setText(String.valueOf(found));
    }

    private void applyFilters() {
        ItemFilterHelper.DateSort activeDateSort =
                dateSortActive ? dateSort : ItemFilterHelper.DateSort.RECENT;
        ItemFilterHelper.LocationSort activeLocationSort =
                locationSortActive ? locationSort : ItemFilterHelper.LocationSort.NONE;

        List<Item> filtered = ItemFilterHelper.apply(
                allItems,
                selectedCategory,
                searchQuery,
                typeFilter,
                activeDateSort,
                nameSort,
                activeLocationSort,
                userLatitude,
                userLongitude);

        adapter.updateItems(filtered);
        binding.layoutEmptyState.setVisibility(
                filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerItems.setVisibility(
                filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
