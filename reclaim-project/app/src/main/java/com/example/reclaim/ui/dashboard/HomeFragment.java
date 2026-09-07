package com.example.reclaim.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;

import com.bumptech.glide.Glide;
import com.example.reclaim.R;
import com.example.reclaim.adapter.ItemAdapter;
import com.example.reclaim.adapter.PreviewItemAdapter;
import com.example.reclaim.data.AppDataRefresh;
import com.example.reclaim.databinding.FragmentHomeBinding;
import com.example.reclaim.ui.profile.PendingClaimsActivity;
import com.example.reclaim.ui.report.ReportActivity;
import com.example.reclaim.model.Claim;
import com.example.reclaim.model.ContactResponse;
import com.example.reclaim.model.Item;
import com.example.reclaim.model.PendingClaimResponse;
import com.example.reclaim.model.User;
import com.example.reclaim.network.ClaimReviewRequest;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.notifications.ClaimDecisionEvents;
import com.example.reclaim.notifications.IncomingClaimEvents;
import com.example.reclaim.ui.profile.ContactInfoDialog;
import com.example.reclaim.util.ConnectionToasts;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Home screen: greeting, nearby items, and possible matches.
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
    private List<PendingClaimResponse> pendingClaims = new ArrayList<>();
    private List<Claim> unacknowledgedDecisions = new ArrayList<>();

    @Nullable
    private androidx.appcompat.app.AlertDialog claimReviewDialog;
    @Nullable
    private String reviewingClaimId;
    @Nullable
    private MaterialTextView reviewItemView;
    @Nullable
    private MaterialTextView reviewQuestionView;
    @Nullable
    private MaterialTextView reviewAnswerView;

    @Nullable
    private androidx.appcompat.app.AlertDialog claimDecisionDialog;
    @Nullable
    private String decidingClaimId;

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

        binding.recyclerNearby.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));

        // Nearby list has no side padding so the fade reaches the edges.
        // Spacing between cards and at the ends is added here.
        int edgeGutter = getResources()
                .getDimensionPixelSize(R.dimen.screen_padding_horizontal);
        int itemSpacing = getResources().getDimensionPixelSize(R.dimen.space_12);
        binding.recyclerNearby.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                boolean isLast = position == state.getItemCount() - 1;
                outRect.left = position == 0 ? edgeGutter : 0;
                outRect.right = isLast ? edgeGutter : itemSpacing;
            }
        });

        binding.recyclerNearby.setAdapter(nearbyAdapter);

        binding.btnShowMore.setOnClickListener(v -> {
            if (requireActivity() instanceof DashboardNavigator navigator)
                navigator.selectTab(R.id.nav_search);
        });

        binding.fabAddReport.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ReportActivity.class)));

        binding.cardPendingClaims.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PendingClaimsActivity.class)));

        binding.cardNewClaim.setOnClickListener(v -> {
            if (!pendingClaims.isEmpty()) {
                showClaimReviewDialog(pendingClaims.get(0));
            }
        });

        binding.cardClaimDecision.setOnClickListener(v -> {
            if (!unacknowledgedDecisions.isEmpty()) {
                showClaimDecisionDialog(unacknowledgedDecisions.get(0));
            }
        });

        fetchLocationIfAllowed();
        observeItems();
        AppDataRefresh.observeNew(getViewLifecycleOwner(), this::refreshFromServer);
        IncomingClaimEvents.live().observe(getViewLifecycleOwner(), tick -> {
            if (tick != null) {
                loadPendingClaims();
            }
        });
        ClaimDecisionEvents.live().observe(getViewLifecycleOwner(), tick -> {
            if (tick != null) {
                loadClaimDecisions();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFromServer();
    }

    /** Reloads home from the server. */
    private void refreshFromServer() {
        viewModel.loadItems();
        loadMyItems();
        loadUserGreeting();
        loadPendingClaims();
        loadClaimDecisions();
    }

    /**
     * Loads claims waiting for review. Hides the card when there are none.
     */
    private void loadPendingClaims() {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) return;

        apiService.getPendingClaimsOnMyItems(authHeader)
                .enqueue(new Callback<List<PendingClaimResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<PendingClaimResponse>> call,
                                   @NonNull Response<List<PendingClaimResponse>> response) {
                if (binding == null || !isAdded()) return;

                pendingClaims = response.isSuccessful() && response.body() != null
                        ? response.body() : new ArrayList<>();
                renderClaimCards();
                syncClaimReviewDialog();
            }

            @Override
            public void onFailure(@NonNull Call<List<PendingClaimResponse>> call,
                                  @NonNull Throwable t) {
                if (binding == null || !isAdded()) return;
                pendingClaims = new ArrayList<>();
                renderClaimCards();
            }
        });
    }

    /**
     * Shows the claim cards only when something is waiting for review.
     */
    private void renderClaimCards() {
        if (binding == null) return;

        int count = pendingClaims.size();
        boolean hasClaims = count > 0;

        binding.cardNewClaim.setVisibility(hasClaims ? View.VISIBLE : View.GONE);
        binding.cardPendingClaims.setVisibility(hasClaims ? View.VISIBLE : View.GONE);
        if (hasClaims) {
            binding.textPendingClaimsCount.setText(getString(
                    R.string.home_pending_claims_subtitle, count));
            PendingClaimResponse latest = pendingClaims.get(0);
            if (!TextUtils.isEmpty(latest.getItemTitle())) {
                binding.textNewClaimSubtitle.setText(latest.getItemTitle());
            } else {
                binding.textNewClaimSubtitle.setText(R.string.home_new_claim_subtitle);
            }
        }
    }

    /**
     * Loads claims the user sent that were reviewed but not yet confirmed.
     */
    private void loadClaimDecisions() {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) return;

        apiService.getMyClaims(authHeader).enqueue(new Callback<List<Claim>>() {
            @Override
            public void onResponse(@NonNull Call<List<Claim>> call,
                                   @NonNull Response<List<Claim>> response) {
                if (binding == null || !isAdded()) return;
                unacknowledgedDecisions = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (Claim claim : response.body()) {
                        if (claim.isDecisionAcknowledged()) {
                            continue;
                        }
                        String status = claim.getStatus();
                        if ("APPROVED".equalsIgnoreCase(status)
                                || "REJECTED".equalsIgnoreCase(status)) {
                            unacknowledgedDecisions.add(claim);
                        }
                    }
                }
                renderDecisionCard();
                syncClaimDecisionDialog();
            }

            @Override
            public void onFailure(@NonNull Call<List<Claim>> call, @NonNull Throwable t) {
                if (binding == null || !isAdded()) return;
                unacknowledgedDecisions = new ArrayList<>();
                renderDecisionCard();
                syncClaimDecisionDialog();
            }
        });
    }

    private void renderDecisionCard() {
        if (binding == null) return;

        boolean hasDecision = !unacknowledgedDecisions.isEmpty();
        binding.cardClaimDecision.setVisibility(hasDecision ? View.VISIBLE : View.GONE);
        if (!hasDecision) {
            return;
        }
        Claim latest = unacknowledgedDecisions.get(0);
        boolean approved = "APPROVED".equalsIgnoreCase(latest.getStatus());
        binding.textClaimDecisionTitle.setText(approved
                ? R.string.home_claim_decision_title_approved
                : R.string.home_claim_decision_title_declined);
        String title = latest.getItemTitle();
        binding.textClaimDecisionSubtitle.setText(
                !TextUtils.isEmpty(title) ? title : getString(R.string.home_claim_decision_subtitle));
    }

    /**
     * Shows the owner's decision. Confirm hides the card. Approve also offers contact info.
     */
    private void showClaimDecisionDialog(Claim claim) {
        decidingClaimId = claim.getId();
        boolean approved = "APPROVED".equalsIgnoreCase(claim.getStatus());
        String itemTitle = !TextUtils.isEmpty(claim.getItemTitle())
                ? claim.getItemTitle() : getString(R.string.item_details_title);
        int titleRes = approved
                ? R.string.home_claim_decision_title_approved
                : R.string.home_claim_decision_title_declined;
        String body = getString(approved
                        ? R.string.claim_decision_approved_body
                        : R.string.claim_decision_declined_body,
                itemTitle);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setMessage(body)
                .setPositiveButton(R.string.btn_confirm, (d, w) ->
                        acknowledgeDecision(claim.getId()));

        if (approved) {
            builder.setNeutralButton(R.string.btn_view_contact_info, null);
        }

        claimDecisionDialog = builder.create();
        claimDecisionDialog.setOnShowListener(d -> {
            if (!approved) {
                return;
            }
            android.widget.Button contact = claimDecisionDialog.getButton(
                    androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);
            contact.setOnClickListener(v -> showOwnerContact(claim.getId()));
        });
        claimDecisionDialog.setOnDismissListener(d -> {
            claimDecisionDialog = null;
            decidingClaimId = null;
        });
        claimDecisionDialog.show();
    }

    private void syncClaimDecisionDialog() {
        if (claimDecisionDialog == null || !claimDecisionDialog.isShowing()
                || decidingClaimId == null) {
            return;
        }
        Claim match = null;
        for (Claim claim : unacknowledgedDecisions) {
            if (decidingClaimId.equals(claim.getId())) {
                match = claim;
                break;
            }
        }
        if (match == null) {
            claimDecisionDialog.dismiss();
            return;
        }
        boolean approved = "APPROVED".equalsIgnoreCase(match.getStatus());
        String itemTitle = !TextUtils.isEmpty(match.getItemTitle())
                ? match.getItemTitle() : getString(R.string.item_details_title);
        claimDecisionDialog.setTitle(getString(approved
                ? R.string.home_claim_decision_title_approved
                : R.string.home_claim_decision_title_declined));
        claimDecisionDialog.setMessage(getString(approved
                        ? R.string.claim_decision_approved_body
                        : R.string.claim_decision_declined_body,
                itemTitle));
    }

    private void acknowledgeDecision(String claimId) {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) return;

        unacknowledgedDecisions.removeIf(c -> claimId.equals(c.getId()));
        renderDecisionCard();

        apiService.acknowledgeClaimDecision(authHeader, claimId)
                .enqueue(new Callback<Claim>() {
                    @Override
                    public void onResponse(@NonNull Call<Claim> call,
                                           @NonNull Response<Claim> response) {
                        if (binding == null || !isAdded()) return;
                        loadClaimDecisions();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
                        if (binding == null || !isAdded()) return;
                        loadClaimDecisions();
                    }
                });
    }

    private void showOwnerContact(String claimId) {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) return;

        apiService.getClaimContact(authHeader, claimId)
                .enqueue(new Callback<ContactResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ContactResponse> call,
                                           @NonNull Response<ContactResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            ContactInfoDialog.show(requireContext(), response.body(),
                                    getViewLifecycleOwner(),
                                    callback -> {
                                        String header = TokenManager.getAuthHeader(requireContext());
                                        if (header == null) {
                                            return;
                                        }
                                        apiService.getClaimContact(header, claimId).enqueue(callback);
                                    });
                        } else {
                            Toast.makeText(requireContext(),
                                    R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ContactResponse> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Asks the owner to approve or decline one incoming claim.
     */
    private void showClaimReviewDialog(PendingClaimResponse claim) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_review_incoming_claim, null);

        reviewItemView = dialogView.findViewById(R.id.text_review_item);
        reviewQuestionView = dialogView.findViewById(R.id.text_review_question);
        reviewAnswerView = dialogView.findViewById(R.id.text_review_answer);
        reviewingClaimId = claim.getClaimId();
        bindClaimReview(claim);

        claimReviewDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.claim_review_title)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_approve_claim, (d, w) ->
                        reviewClaim(claim.getClaimId(), "APPROVED"))
                .setNegativeButton(R.string.btn_decline_claim, (d, w) ->
                        reviewClaim(claim.getClaimId(), "REJECTED"))
                .create();
        claimReviewDialog.setOnDismissListener(d -> {
            claimReviewDialog = null;
            reviewingClaimId = null;
            reviewItemView = null;
            reviewQuestionView = null;
            reviewAnswerView = null;
        });
        claimReviewDialog.show();
    }

    private void bindClaimReview(@NonNull PendingClaimResponse claim) {
        if (reviewItemView == null || reviewQuestionView == null || reviewAnswerView == null) {
            return;
        }
        reviewItemView.setText(!TextUtils.isEmpty(claim.getItemTitle())
                ? claim.getItemTitle() : getString(R.string.item_details_title));
        reviewQuestionView.setText(!TextUtils.isEmpty(claim.getVerificationQuestion())
                ? claim.getVerificationQuestion()
                : getString(R.string.claim_default_question));
        reviewAnswerView.setText(!TextUtils.isEmpty(claim.getValidationAnswer())
                ? claim.getValidationAnswer() : getString(R.string.placeholder_em_dash));
    }

    private void syncClaimReviewDialog() {
        if (claimReviewDialog == null || !claimReviewDialog.isShowing()
                || reviewingClaimId == null) {
            return;
        }
        PendingClaimResponse match = null;
        for (PendingClaimResponse claim : pendingClaims) {
            if (reviewingClaimId.equals(claim.getClaimId())) {
                match = claim;
                break;
            }
        }
        if (match == null) {
            claimReviewDialog.dismiss();
            return;
        }
        bindClaimReview(match);
    }

    /**
     * Sends the owner's decision. The card is removed right away, then the list is refreshed.
     */
    private void reviewClaim(String claimId, String status) {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) return;

        List<PendingClaimResponse> snapshot = new ArrayList<>(pendingClaims);
        pendingClaims.removeIf(c -> claimId.equals(c.getClaimId()));
        renderClaimCards();

        apiService.reviewClaim(authHeader, claimId, new ClaimReviewRequest(status))
                .enqueue(new Callback<Claim>() {
            @Override
            public void onResponse(@NonNull Call<Claim> call, @NonNull Response<Claim> response) {
                if (binding == null || !isAdded()) return;
                Toast.makeText(requireContext(),
                        response.isSuccessful()
                                ? R.string.msg_claim_reviewed
                                : R.string.msg_claim_review_failed,
                        Toast.LENGTH_SHORT).show();
                if (response.isSuccessful()) {
                    loadPendingClaims();
                } else {
                    pendingClaims = snapshot;
                    renderClaimCards();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Claim> call, @NonNull Throwable t) {
                if (binding == null || !isAdded()) return;
                pendingClaims = snapshot;
                renderClaimCards();
                Toast.makeText(requireContext(),
                        R.string.msg_claim_review_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserGreeting() {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) return;

        apiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (binding == null || !isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    String name = response.body().getName();

                    if (name != null && !name.isEmpty())
                        binding.textGreeting.setText(getString(R.string.dashboard_greeting_named, name));

                    loadAvatar(response.body().getProfilePictureUrl());
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                // Keep the default greeting
            }
        });
    }

    private void loadAvatar(@Nullable String url) {
        if (binding == null || url == null || url.isEmpty()) return;

        binding.imageHomeAvatar.setPadding(0, 0, 0, 0);
        binding.imageHomeAvatar.setImageTintList(null);
        Glide.with(this)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_settings_person)
                .error(R.drawable.ic_settings_person)
                .into(binding.imageHomeAvatar);
    }

    private void loadMyItems() {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) return;

        apiService.getMyItems(authHeader).enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(@NonNull Call<List<Item>> call,
                                   @NonNull Response<List<Item>> response) {
                if (binding == null || !isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    myItems = response.body();
                    renderMatches(viewModel.getItems().getValue());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
                // Leave matches empty
            }
        });
    }

    private void fetchLocationIfAllowed() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
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
            if (loading != null && binding != null)
                binding.progressHome.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty())
                ConnectionToasts.showIfOnline(requireContext(), message, Toast.LENGTH_SHORT);
        });

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            renderNearby(items);
            renderMatches(items);
        });
    }

    private void renderNearby(List<Item> items) {
        if (binding == null) return;

        List<Item> source = items != null ? items : new ArrayList<>();
        List<Item> preview = ItemMatchHelper.nearbyPreview(
                source, userLatitude, userLongitude, NEARBY_PREVIEW_LIMIT);

        nearbyAdapter.updateItems(preview);
        boolean empty = preview.isEmpty();

        binding.textNearbyEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerNearby.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.btnShowMore.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void renderMatches(List<Item> allItems) {
        if (binding == null) return;

        binding.layoutMatches.removeAllViews();
        List<Item> lostItems = new ArrayList<>();

        for (Item item : myItems)
            if ("LOST".equalsIgnoreCase(item.getType())) lostItems.add(item);

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
            header.setPadding(0,
                    getResources().getDimensionPixelSize(R.dimen.space_16),
                    0,
                    getResources().getDimensionPixelSize(R.dimen.space_10));
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
        if (claimReviewDialog != null) {
            claimReviewDialog.dismiss();
        }
        if (claimDecisionDialog != null) {
            claimDecisionDialog.dismiss();
        }
        super.onDestroyView();
        binding = null;
    }
}
