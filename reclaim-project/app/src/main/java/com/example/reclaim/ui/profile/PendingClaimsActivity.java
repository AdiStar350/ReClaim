package com.example.reclaim.ui.profile;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.reclaim.R;
import com.example.reclaim.adapter.PendingClaimAdapter;
import com.example.reclaim.databinding.ActivityPendingClaimsBinding;
import com.example.reclaim.model.Claim;
import com.example.reclaim.model.PendingClaimResponse;
import com.example.reclaim.network.ClaimReviewRequest;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PendingClaimsActivity extends AppCompatActivity {

    private ActivityPendingClaimsBinding binding;
    private ReClaimApiService apiService;
    private PendingClaimAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPendingClaimsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getApiService();

        setSupportActionBar(binding.toolbarPendingClaims);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarPendingClaims.setNavigationOnClickListener(v -> finish());

        adapter = new PendingClaimAdapter(new PendingClaimAdapter.ReviewListener() {
            @Override
            public void onApprove(PendingClaimResponse claim) {
                reviewClaim(claim.getClaimId(), "APPROVED");
            }

            @Override
            public void onReject(PendingClaimResponse claim) {
                reviewClaim(claim.getClaimId(), "REJECTED");
            }
        });

        binding.recyclerPendingClaims.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerPendingClaims.setAdapter(adapter);

        loadPendingClaims();
    }

    private void loadPendingClaims() {
        String authHeader = TokenManager.getAuthHeader(this);
        if (authHeader == null) {
            finish();
            return;
        }

        apiService.getPendingClaimsOnMyItems(authHeader).enqueue(new Callback<List<PendingClaimResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<PendingClaimResponse>> call,
                                   @NonNull Response<List<PendingClaimResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateClaims(response.body());
                } else {
                    Toast.makeText(PendingClaimsActivity.this,
                            R.string.msg_load_pending_claims_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PendingClaimResponse>> call,
                                  @NonNull Throwable t) {
                Toast.makeText(PendingClaimsActivity.this,
                        R.string.msg_load_pending_claims_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reviewClaim(String claimId, String status) {
        String authHeader = TokenManager.getAuthHeader(this);
        if (authHeader == null) {
            return;
        }

        apiService.reviewClaim(authHeader, claimId, new ClaimReviewRequest(status))
                .enqueue(new Callback<Claim>() {
                    @Override
                    public void onResponse(@NonNull Call<Claim> call,
                                           @NonNull Response<Claim> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(PendingClaimsActivity.this,
                                    R.string.msg_claim_reviewed, Toast.LENGTH_SHORT).show();
                            loadPendingClaims();
                        } else {
                            Toast.makeText(PendingClaimsActivity.this,
                                    R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
                        Toast.makeText(PendingClaimsActivity.this,
                                R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
