package com.example.reclaim.ui.myitems;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.reclaim.R;
import com.example.reclaim.adapter.MyReportAdapter;
import com.example.reclaim.databinding.FragmentMyItemsBinding;
import com.example.reclaim.model.Item;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.ui.report.ReportActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lists the user's lost and found reports with edit and delete actions.
 */
public class MyItemsFragment extends Fragment implements MyReportAdapter.Listener {

    private FragmentMyItemsBinding binding;
    private ReClaimApiService apiService;
    private MyReportAdapter adapter;
    private List<Item> allMyItems = new ArrayList<>();
    private int currentTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyItemsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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

        binding.fabAddReport.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ReportActivity.class);
            startActivity(intent);
        });

        loadMyItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyItems();
    }

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
                    applyTabFilter();
                } else {
                    Toast.makeText(requireContext(),
                            R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
                if (binding == null || !isAdded()) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        R.string.error_generic, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyTabFilter() {
        List<Item> filtered = new ArrayList<>();
        for (Item item : allMyItems) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
