package com.example.reclaim.ui.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.reclaim.R;
import com.example.reclaim.adapter.PendingClaimAdapter;
import com.example.reclaim.databinding.DialogPendingClaimsBinding;
import com.example.reclaim.model.Claim;
import com.example.reclaim.model.ContactResponse;
import com.example.reclaim.model.PendingClaimResponse;
import com.example.reclaim.network.ClaimReviewRequest;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Bottom sheet listing every pending claim the signed-in owner still needs to review.
 */
public final class PendingClaimsSheet {

    public interface Listener {
        void onClaimsChanged(@NonNull List<PendingClaimResponse> remaining);

        void onSheetDismissed();
    }

    private final Fragment host;
    private final Listener listener;
    private final BottomSheetDialog dialog;
    private final DialogPendingClaimsBinding binding;
    private final PendingClaimAdapter adapter;
    private final ReClaimApiService apiService;
    private boolean notifyHostOnDismiss = true;
    private boolean reviewing;

    private PendingClaimsSheet(@NonNull Fragment host, @NonNull Listener listener) {
        this.host = host;
        this.listener = listener;
        this.apiService = RetrofitClient.getApiService();

        Context context = host.requireContext();
        binding = DialogPendingClaimsBinding.inflate(LayoutInflater.from(context));
        adapter = new PendingClaimAdapter(new PendingClaimAdapter.ReviewListener() {
            @Override
            public void onApprove(PendingClaimResponse claim) {
                reviewClaim(claim, true);
            }

            @Override
            public void onReject(PendingClaimResponse claim) {
                confirmReject(claim);
            }
        });

        binding.recyclerPendingClaims.setLayoutManager(new LinearLayoutManager(context));
        binding.recyclerPendingClaims.setAdapter(adapter);
        binding.recyclerPendingClaims.setNestedScrollingEnabled(true);
        ViewGroup.LayoutParams listParams = binding.recyclerPendingClaims.getLayoutParams();
        listParams.height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.55f);
        binding.recyclerPendingClaims.setLayoutParams(listParams);

        dialog = new BottomSheetDialog(context);
        dialog.setContentView(binding.getRoot());
        dialog.setOnShowListener(d -> expandSheet());
        dialog.setOnDismissListener(d -> {
            if (notifyHostOnDismiss) {
                listener.onSheetDismissed();
            }
        });
    }

    @NonNull
    public static PendingClaimsSheet show(@NonNull Fragment host,
                                          @NonNull List<PendingClaimResponse> claims,
                                          @NonNull Listener listener) {
        PendingClaimsSheet sheet = new PendingClaimsSheet(host, listener);
        sheet.replaceClaims(claims);
        sheet.dialog.show();
        return sheet;
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }

    public void dismiss() {
        dismiss(true);
    }

    public void dismissQuietly() {
        dismiss(false);
    }

    private void dismiss(boolean notifyHost) {
        notifyHostOnDismiss = notifyHost;
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void replaceClaims(@NonNull List<PendingClaimResponse> claims) {
        if (reviewing) {
            return;
        }
        adapter.updateClaims(new ArrayList<>(claims));
        updateHeader();
    }

    private void expandSheet() {
        FrameLayout bottomSheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void updateHeader() {
        binding.textPendingClaimsHeader.setText(host.getString(
                R.string.pending_claims_sheet_title, adapter.getItemCount()));
    }

    private void confirmReject(PendingClaimResponse claim) {
        if (!host.isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(host.requireContext())
                .setTitle(R.string.claim_reject_confirm_title)
                .setMessage(R.string.claim_reject_confirm_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_reject_claim, (d, w) -> reviewClaim(claim, false))
                .show();
    }

    private void reviewClaim(PendingClaimResponse claim, boolean approve) {
        if (!host.isAdded() || claim.getClaimId() == null) {
            return;
        }
        String authHeader = TokenManager.getAuthHeader(host.requireContext());
        if (authHeader == null) {
            return;
        }

        reviewing = true;
        adapter.setBusyClaimId(claim.getClaimId());
        String status = approve ? "APPROVED" : "REJECTED";
        apiService.reviewClaim(authHeader, claim.getClaimId(), new ClaimReviewRequest(status))
                .enqueue(new Callback<Claim>() {
                    @Override
                    public void onResponse(@NonNull Call<Claim> call,
                                           @NonNull Response<Claim> response) {
                        if (!host.isAdded()) {
                            return;
                        }
                        if (!response.isSuccessful()) {
                            reviewing = false;
                            adapter.setBusyClaimId(null);
                            Toast.makeText(host.requireContext(),
                                    R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (approve) {
                            revealClaimantContact(claim.getClaimId());
                            adapter.removeClaimsForItem(claim.getItemId());
                        } else {
                            adapter.removeClaim(claim.getClaimId());
                        }
                        reviewing = false;
                        adapter.setBusyClaimId(null);
                        afterListChanged();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
                        if (!host.isAdded()) {
                            return;
                        }
                        reviewing = false;
                        adapter.setBusyClaimId(null);
                        Toast.makeText(host.requireContext(),
                                R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void afterListChanged() {
        updateHeader();
        listener.onClaimsChanged(adapter.getClaims());
        if (adapter.getItemCount() == 0) {
            dismiss();
        }
    }

    private void revealClaimantContact(@Nullable String claimId) {
        if (claimId == null || !host.isAdded()) {
            return;
        }
        String authHeader = TokenManager.getAuthHeader(host.requireContext());
        if (authHeader == null) {
            return;
        }

        apiService.getClaimContact(authHeader, claimId)
                .enqueue(new Callback<ContactResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ContactResponse> call,
                                           @NonNull Response<ContactResponse> response) {
                        if (!host.isAdded()) {
                            return;
                        }
                        if (response.isSuccessful() && response.body() != null) {
                            ContactInfoDialog.show(host.requireContext(), response.body(),
                                    host.getViewLifecycleOwner(),
                                    callback -> {
                                        String header = TokenManager.getAuthHeader(
                                                host.requireContext());
                                        if (header == null) {
                                            return;
                                        }
                                        apiService.getClaimContact(header, claimId)
                                                .enqueue(callback);
                                    },
                                    R.string.contact_claimant_title);
                        } else {
                            Toast.makeText(host.requireContext(),
                                    R.string.msg_contact_unavailable, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ContactResponse> call,
                                          @NonNull Throwable t) {
                        if (!host.isAdded()) {
                            return;
                        }
                        Toast.makeText(host.requireContext(),
                                R.string.msg_contact_unavailable, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
