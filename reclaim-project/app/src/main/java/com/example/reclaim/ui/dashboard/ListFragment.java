package com.example.reclaim.ui.dashboard;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.reclaim.adapter.ItemAdapter;
import com.example.reclaim.databinding.FragmentListBinding;
import com.example.reclaim.model.Item;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that displays a filterable, searchable list of lost and found items.
 */
public class ListFragment extends Fragment {

    private FragmentListBinding binding;
    private ItemsViewModel viewModel;
    private ItemAdapter adapter;
    private List<Item> allItems = new ArrayList<>();
    private String selectedCategory = null;
    private String searchQuery = "";

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

        setupRecyclerView();
        setupChipGroup();
        setupSearch();
        setupSwipeRefresh();
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
    }

    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase(Locale.US) : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadItems());
    }

    private void observeViewModel() {
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
    }

    private void applyFilters() {
        List<Item> filtered = new ArrayList<>();
        for (Item item : allItems) {
            if (selectedCategory != null
                    && (item.getCategory() == null
                    || !selectedCategory.equalsIgnoreCase(item.getCategory()))) {
                continue;
            }

            if (!searchQuery.isEmpty()) {
                String haystack = ((item.getTitle() != null ? item.getTitle() : "") + " "
                        + (item.getDescription() != null ? item.getDescription() : "") + " "
                        + (item.getLocation() != null ? item.getLocation() : ""))
                        .toLowerCase(Locale.US);
                if (!haystack.contains(searchQuery)) {
                    continue;
                }
            }

            filtered.add(item);
        }
        adapter.updateItems(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
