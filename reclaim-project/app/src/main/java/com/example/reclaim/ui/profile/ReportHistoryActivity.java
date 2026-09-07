package com.example.reclaim.ui.profile;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.reclaim.R;
import com.example.reclaim.adapter.HistoryAdapter;
import com.example.reclaim.data.AppDataRefresh;
import com.example.reclaim.databinding.ActivityReportHistoryBinding;
import com.example.reclaim.model.Claim;
import com.example.reclaim.model.ContactResponse;
import com.example.reclaim.model.Item;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lists reports the user made or claimed, with a simple status for each.
 */
public class ReportHistoryActivity extends AppCompatActivity {

    private ActivityReportHistoryBinding binding;
    private ReClaimApiService apiService;
    private HistoryAdapter adapter;

    private final List<HistoryAdapter.Entry> reportEntries = new ArrayList<>();
    private final List<HistoryAdapter.Entry> claimEntries = new ArrayList<>();
    private int pendingRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityReportHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getApiService();

        setSupportActionBar(binding.toolbarReportHistory);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarReportHistory.setNavigationOnClickListener(v -> finish());

        adapter = new HistoryAdapter(this::showOwnerContact);
        binding.recyclerReportHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerReportHistory.setAdapter(adapter);

        AppDataRefresh.observeNew(this, this::loadHistory);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        String authHeader = TokenManager.getAuthHeader(this);
        if (authHeader == null) {
            finish();
            return;
        }

        reportEntries.clear();
        claimEntries.clear();
        pendingRequests = 2;
        binding.progressReportHistory.setVisibility(View.VISIBLE);
        binding.textHistoryEmpty.setVisibility(View.GONE);

        apiService.getMyItems(authHeader).enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(@NonNull Call<List<Item>> call,
                                   @NonNull Response<List<Item>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Item item : response.body()) {
                        reportEntries.add(new HistoryAdapter.Entry(
                                item.getTitle(),
                                item.getImageUrl(),
                                R.string.history_role_reported,
                                statusForReport(item)));
                    }
                }
                onSectionLoaded();
            }

            @Override
            public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
                onSectionLoaded();
            }
        });

        apiService.getMyClaims(authHeader).enqueue(new Callback<List<Claim>>() {
            @Override
            public void onResponse(@NonNull Call<List<Claim>> call,
                                   @NonNull Response<List<Claim>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Claim claim : response.body()) {
                        HistoryAdapter.Status status = statusForClaim(claim);
                        boolean approvedClaim = status == HistoryAdapter.Status.APPROVED;
                        claimEntries.add(new HistoryAdapter.Entry(
                                claim.getItemTitle(),
                                claim.getItemImageUrl(),
                                R.string.history_role_claimed,
                                status,
                                claim.getId(),
                                approvedClaim));
                    }
                }
                onSectionLoaded();
            }

            @Override
            public void onFailure(@NonNull Call<List<Claim>> call, @NonNull Throwable t) {
                onSectionLoaded();
            }
        });
    }

    /**
     * Called when one of the two requests finishes. Draws the list when both are done.
     */
    private void onSectionLoaded() {
        if (binding == null || isFinishing()) {
            return;
        }
        pendingRequests--;
        if (pendingRequests > 0) {
            return;
        }

        binding.progressReportHistory.setVisibility(View.GONE);

        List<HistoryAdapter.Entry> combined = new ArrayList<>();
        combined.addAll(reportEntries);
        combined.addAll(claimEntries);
        adapter.submit(combined);

        binding.textHistoryEmpty.setVisibility(
                combined.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Open reports are pending. Closed ones are approved. */
    private HistoryAdapter.Status statusForReport(Item item) {
        return "CLOSED".equalsIgnoreCase(item.getStatus())
                ? HistoryAdapter.Status.APPROVED
                : HistoryAdapter.Status.PENDING;
    }

    /** Status badge for a claim. */
    private HistoryAdapter.Status statusForClaim(Claim claim) {
        String status = claim.getStatus();
        if ("APPROVED".equalsIgnoreCase(status)) {
            return HistoryAdapter.Status.APPROVED;
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            return HistoryAdapter.Status.DECLINED;
        }
        return HistoryAdapter.Status.PENDING;
    }

    /**
     * Loads the owner's name, email, and phone for an approved claim.
     */
    private void showOwnerContact(String claimId) {
        String authHeader = TokenManager.getAuthHeader(this);
        if (authHeader == null) {
            return;
        }
        apiService.getClaimContact(authHeader, claimId)
                .enqueue(new Callback<ContactResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ContactResponse> call,
                                           @NonNull Response<ContactResponse> response) {
                        if (isFinishing()) {
                            return;
                        }
                        if (response.isSuccessful() && response.body() != null) {
                            ContactInfoDialog.show(ReportHistoryActivity.this, response.body(),
                                    ReportHistoryActivity.this,
                                    callback -> {
                                        String header = TokenManager.getAuthHeader(
                                                ReportHistoryActivity.this);
                                        if (header == null) {
                                            return;
                                        }
                                        apiService.getClaimContact(header, claimId).enqueue(callback);
                                    });
                        } else {
                            Toast.makeText(ReportHistoryActivity.this,
                                    R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ContactResponse> call,
                                          @NonNull Throwable t) {
                        if (isFinishing()) {
                            return;
                        }
                        Toast.makeText(ReportHistoryActivity.this,
                                R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
