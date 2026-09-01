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
    private List<PendingClaimResponse> pendingClaims = new ArrayList<>();
    private List<Claim> unacknowledgedDecisions = new ArrayList<>();

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

        // The RecyclerView has no horizontal padding (so the fading edge spans
        // the true view edges); all spacing is supplied here instead. Edge
        // gutters keep the list inset like the rest of the screen, and the
        // between-item spacing replaces the card's old marginEnd.
        int edgeGutter = getResources()
                .getDimensionPixelSize(R.dimen.screen_padding_horizontal);
        int itemSpacing = Math.round(12 * getResources().getDisplayMetrics().density);
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
        loadMyItems();
        observeItems();
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

        if (viewModel.getItems().getValue() == null
                || viewModel.getItems().getValue().isEmpty())
            viewModel.loadItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload here so a profile picture (or name) changed on the settings
        // screen is reflected when the user returns to the dashboard.
        loadUserGreeting();
        loadPendingClaims();
        loadClaimDecisions();
    }

    /**
     * Loads claims awaiting the user's review and toggles the top-of-dashboard
     * "Review Pending Claims" card accordingly (hidden when there are none).
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
     * Toggles the two claim cards based on the current pending-claim list: the
     * high-priority alert and the "Review Pending Claims" shortcut appear only
     * when at least one claim awaits review.
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
     * Loads claims the current user submitted that have been reviewed but
     * not yet confirmed, then toggles the decision card at the top of home.
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
            }

            @Override
            public void onFailure(@NonNull Call<List<Claim>> call, @NonNull Throwable t) {
                if (binding == null || !isAdded()) return;
                unacknowledgedDecisions = new ArrayList<>();
                renderDecisionCard();
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
     * Shows the owner's decision. Decline: Confirm dismisses the card.
     * Approve: Confirm plus View Contact Info (owner name/email/phone).
     */
    private void showClaimDecisionDialog(Claim claim) {
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

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            if (!approved) {
                return;
            }
            android.widget.Button contact = dialog.getButton(
                    androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);
            contact.setOnClickListener(v -> showOwnerContact(claim.getId()));
        });
        dialog.show();
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
                            ContactInfoDialog.show(requireContext(), response.body());
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
     * Shows a quick approve/decline dialog for a single incoming claim,
     * displaying the owner's verification question and the claimant's answer.
     */
    private void showClaimReviewDialog(PendingClaimResponse claim) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_review_incoming_claim, null);

        MaterialTextView itemView = dialogView.findViewById(R.id.text_review_item);
        MaterialTextView questionView = dialogView.findViewById(R.id.text_review_question);
        MaterialTextView answerView = dialogView.findViewById(R.id.text_review_answer);

        itemView.setText(!TextUtils.isEmpty(claim.getItemTitle())
                ? claim.getItemTitle() : getString(R.string.item_details_title));
        questionView.setText(!TextUtils.isEmpty(claim.getVerificationQuestion())
                ? claim.getVerificationQuestion()
                : getString(R.string.claim_default_question));
        answerView.setText(!TextUtils.isEmpty(claim.getValidationAnswer())
                ? claim.getValidationAnswer() : "—");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.claim_review_title)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_approve_claim, (d, w) ->
                        reviewClaim(claim.getClaimId(), "APPROVED"))
                .setNegativeButton(R.string.btn_decline_claim, (d, w) ->
                        reviewClaim(claim.getClaimId(), "REJECTED"))
                .show();
    }

    /**
     * Submits the owner's decision. The backend records the correct
     * closureReason ({@code claim approved} or {@code claim declined}).
     * The incoming-claim card is removed immediately, then the list is
     * refreshed from the server.
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
                // Keep default greeting
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
                // Matches section stays empty
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
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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
            header.setPadding(0, 16, 0, 10);
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
