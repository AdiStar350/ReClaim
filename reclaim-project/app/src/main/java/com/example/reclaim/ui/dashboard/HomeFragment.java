package com.example.reclaim.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.reclaim.R;
import com.example.reclaim.adapter.ItemAdapter;
import com.example.reclaim.adapter.PreviewItemAdapter;
import com.example.reclaim.databinding.FragmentHomeBinding;
import com.example.reclaim.model.Item;
import com.example.reclaim.model.User;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dashboard home: welcome message, nearby items preview, and possible matches.
 */
public class HomeFragment extends Fragment {

    private static final int NEARBY_PREVIEW_LIMIT = 4;

    private FragmentHomeBinding binding;
    private ItemsViewModel viewModel;
    private ReClaimApiService apiService;
    private PreviewItemAdapter nearbyAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private Double userLatitude;
    private Double userLongitude;
    private List<Item> myItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ItemsViewModel.class);
        apiService = RetrofitClient.getApiService();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        nearbyAdapter = new PreviewItemAdapter(item ->
                startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item)));
        binding.recyclerNearby.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerNearby.setAdapter(nearbyAdapter);

        binding.btnShowMore.setOnClickListener(v -> {
            if (requireActivity() instanceof DashboardNavigator navigator) {
                navigator.selectTab(R.id.nav_search);
            }
        });

        loadUserGreeting();
        fetchLocationIfAllowed();
        loadMyItems();
        observeItems();

        if (viewModel.getItems().getValue() == null
                || viewModel.getItems().getValue().isEmpty()) {
            viewModel.loadItems();
        }
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

    private void loadMyItems() {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) {
            return;
        }
        apiService.getMyItems(authHeader).enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(@NonNull Call<List<Item>> call,
                                   @NonNull Response<List<Item>> response) {
                if (binding == null || !isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    myItems = response.body();
                    renderMatches(viewModel.getItems().getValue());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
                // Matches section stays empty
            }
        });
    }

    private void fetchLocationIfAllowed() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            userLatitude = location.getLatitude();
                            userLongitude = location.getLongitude();
                            renderNearby(viewModel.getItems().getValue());
                        }
                    });
        }
    }

    private void observeItems() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && binding != null) {
                binding.progressHome.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            renderNearby(items);
            renderMatches(items);
        });
    }

    private void renderNearby(List<Item> items) {
        if (binding == null) {
            return;
        }
        List<Item> source = items != null ? items : new ArrayList<>();
        List<Item> preview = ItemMatchHelper.nearbyPreview(
                source, userLatitude, userLongitude, NEARBY_PREVIEW_LIMIT);
        nearbyAdapter.updateItems(preview);
        boolean empty = preview.isEmpty();
        binding.textNearbyEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerNearby.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void renderMatches(List<Item> allItems) {
        if (binding == null) {
            return;
        }
        binding.layoutMatches.removeAllViews();

        List<Item> lostItems = new ArrayList<>();
        for (Item item : myItems) {
            if ("LOST".equalsIgnoreCase(item.getType())) {
                lostItems.add(item);
            }
        }

        String userId = TokenManager.getUserId(requireContext());
        List<ItemMatchHelper.MatchGroup> groups = ItemMatchHelper.buildMatchGroups(
                allItems != null ? allItems : new ArrayList<>(), lostItems, userId);

        if (groups.isEmpty()) {
            binding.textMatchesEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.textMatchesEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (ItemMatchHelper.MatchGroup group : groups) {
            MaterialTextView header = new MaterialTextView(requireContext());
            header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
            header.setTextColor(requireContext().getColor(R.color.brand_dark));
            header.setPadding(0, 16, 0, 8);
            header.setText(getString(R.string.home_match_for, group.lostItem.getTitle()));
            binding.layoutMatches.addView(header);

            androidx.recyclerview.widget.RecyclerView recycler =
                    new androidx.recyclerview.widget.RecyclerView(requireContext());
            recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            recycler.setNestedScrollingEnabled(false);
            ItemAdapter adapter = new ItemAdapter(group.matches, item ->
                    startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item)));
            recycler.setAdapter(adapter);
            binding.layoutMatches.addView(recycler);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
