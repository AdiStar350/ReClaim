package com.example.reclaim.ui.details;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.reclaim.R;
import com.example.reclaim.data.AppDataRefresh;
import com.example.reclaim.databinding.ActivityItemDetailsBinding;
import com.example.reclaim.model.Claim;
import com.example.reclaim.model.ContactResponse;
import com.example.reclaim.model.Item;
import com.example.reclaim.network.ClaimRequest;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.ui.profile.ContactInfoDialog;
import com.example.reclaim.util.ConnectionToasts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shows one lost or found item: photo, details, and a claim button.
 */
public class ItemDetailsActivity extends AppCompatActivity {

    /** Extra key for the item id. */
    public static final String EXTRA_ITEM_ID = "ITEM_ID";
    public static final String EXTRA_ITEM_TITLE = "ITEM_TITLE";
    public static final String EXTRA_ITEM_DESCRIPTION = "ITEM_DESCRIPTION";
    public static final String EXTRA_ITEM_LOCATION = "ITEM_LOCATION";
    public static final String EXTRA_ITEM_STATUS = "ITEM_STATUS";
    public static final String EXTRA_ITEM_CATEGORY = "ITEM_CATEGORY";
    public static final String EXTRA_ITEM_IMAGE_URL = "ITEM_IMAGE_URL";
    public static final String EXTRA_ITEM_TYPE = "ITEM_TYPE";
    public static final String EXTRA_ITEM_VERIFICATION_QUESTION = "ITEM_VERIFICATION_QUESTION";
    public static final String EXTRA_ITEM_OWNER_ID = "ITEM_OWNER_ID";
    public static final String EXTRA_ITEM_REPORTED_AT = "ITEM_REPORTED_AT";
    public static final String EXTRA_ITEM_OWNER_NAME = "ITEM_OWNER_NAME";
    public static final String EXTRA_ITEM_OWNER_PROFILE_URL = "ITEM_OWNER_PROFILE_URL";

    private ActivityItemDetailsBinding binding;
    private ReClaimApiService apiService;

    /** Item id, used when sending a claim. */
    private String itemId;
    private String verificationQuestion;

    /** Who created this report. */
    private String ownerId;
    private String itemType;
    private boolean claimSubmitting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityItemDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getApiService();

        setupToolbar();
        populateFromIntent();
        setupBottomAction();
        AppDataRefresh.observeNew(this, this::refreshFromServer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFromServer();
    }

    /**
     * Sets the toolbar and a back button.
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbarItemDetails);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // Title sits below the photo, not on it.
        binding.collapsingToolbar.setTitleEnabled(false);
    }

    /**
     * Fills the screen from the extras. Uses placeholder text if something is missing.
     */
    private void populateFromIntent() {
        itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        verificationQuestion = getIntent().getStringExtra(EXTRA_ITEM_VERIFICATION_QUESTION);
        ownerId = getIntent().getStringExtra(EXTRA_ITEM_OWNER_ID);
        itemType = getIntent().getStringExtra(EXTRA_ITEM_TYPE);

        bindFields(
                getIntent().getStringExtra(EXTRA_ITEM_TITLE),
                getIntent().getStringExtra(EXTRA_ITEM_DESCRIPTION),
                getIntent().getStringExtra(EXTRA_ITEM_LOCATION),
                getIntent().getStringExtra(EXTRA_ITEM_STATUS),
                getIntent().getStringExtra(EXTRA_ITEM_CATEGORY),
                getIntent().getStringExtra(EXTRA_ITEM_IMAGE_URL),
                getIntent().getStringExtra(EXTRA_ITEM_REPORTED_AT),
                getIntent().getStringExtra(EXTRA_ITEM_OWNER_NAME),
                getIntent().getStringExtra(EXTRA_ITEM_OWNER_PROFILE_URL));
    }

    private void refreshFromServer() {
        if (TextUtils.isEmpty(itemId)) {
            return;
        }
        String authHeader = TokenManager.getAuthHeader(this);
        if (authHeader == null) {
            return;
        }
        apiService.getItemById(authHeader, itemId).enqueue(new Callback<Item>() {
            @Override
            public void onResponse(@NonNull Call<Item> call, @NonNull Response<Item> response) {
                if (isFinishing()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    applyItem(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Item> call, @NonNull Throwable t) {
                // Keep what was already shown.
            }
        });
    }

    private void applyItem(@NonNull Item item) {
        if (!TextUtils.isEmpty(item.getId())) {
            itemId = item.getId();
        }
        verificationQuestion = item.getVerificationQuestion();
        ownerId = item.getOwnerId();
        itemType = item.getType();
        bindFields(
                item.getTitle(),
                item.getDescription(),
                item.getLocation(),
                item.getStatus(),
                item.getCategory(),
                item.getImageUrl(),
                item.getReportedAt(),
                item.getOwnerName(),
                item.getOwnerProfilePictureUrl());
        setupBottomAction();
    }

    private void bindFields(@Nullable String title,
                            @Nullable String description,
                            @Nullable String location,
                            @Nullable String status,
                            @Nullable String category,
                            @Nullable String imageUrl,
                            @Nullable String reportedAt,
                            @Nullable String ownerName,
                            @Nullable String ownerProfileUrl) {
        // Title under the photo
        binding.textItemTitle.setText(
                title != null ? title : getString(R.string.item_details_title));

        // What it is
        binding.textDescription.setText(
                description != null ? description : getString(R.string.placeholder_no_description));

        // Where it was
        binding.textLocation.setText(
                location != null ? location : getString(R.string.placeholder_location_unspecified));

        // When
        binding.textDateFound.setText(formatReportedDate(reportedAt));

        // Who reported it
        binding.textReportedBy.setText(
                !TextUtils.isEmpty(ownerName) ? ownerName : getString(R.string.placeholder_unknown_reporter));
        Glide.with(this)
                .load(!TextUtils.isEmpty(ownerProfileUrl) ? ownerProfileUrl : null)
                .centerCrop()
                .placeholder(R.drawable.ic_settings_person)
                .error(R.drawable.ic_settings_person)
                .into(binding.imageReporterAvatar);

        // Status and category
        if ("CLOSED".equalsIgnoreCase(status)) {
            binding.chipStatus.setText(R.string.status_closed);
        } else if ("OPEN".equalsIgnoreCase(status)) {
            binding.chipStatus.setText(R.string.status_open);
        } else {
            binding.chipStatus.setText(status != null ? status : getString(R.string.placeholder_unknown));
        }
        binding.chipCategory.setText(
                category != null ? category : getString(R.string.placeholder_general_category));

        // Photo
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_item_placeholder)
                    .error(R.drawable.ic_item_placeholder)
                    .into(binding.imageItem);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_item_placeholder)
                    .centerCrop()
                    .into(binding.imageItem);
        }
    }

    /**
     * Turns a timestamp into a short date. If that fails, it uses the raw text.
     */
    private String formatReportedDate(String isoTimestamp) {
        if (TextUtils.isEmpty(isoTimestamp)) {
            return getString(R.string.placeholder_date_unavailable);
        }
        try {
            String trimmed = isoTimestamp.length() >= 19
                    ? isoTimestamp.substring(0, 19) : isoTimestamp;
            SimpleDateFormat parser =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = parser.parse(trimmed);
            if (parsed != null) {
                SimpleDateFormat formatter =
                        new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                return formatter.format(parsed);
            }
        } catch (Exception ignored) {
            // Use the raw date below.
        }
        return isoTimestamp.length() >= 10 ? isoTimestamp.substring(0, 10) : isoTimestamp;
    }

    /**
     * Lost reports show the reporter's contact. Found reports show a claim button, hidden if you own it.
     */
    private void setupBottomAction() {
        if (claimSubmitting) {
            return;
        }
        if (isCurrentUserOwner()) {
            binding.bottomClaimContainer.setVisibility(View.GONE);
            return;
        }
        binding.bottomClaimContainer.setVisibility(View.VISIBLE);
        if ("LOST".equalsIgnoreCase(itemType)) {
            binding.btnClaim.setText(R.string.btn_show_contact_info);
            binding.btnClaim.setOnClickListener(v -> loadReporterContact());
            return;
        }
        binding.btnClaim.setText(R.string.btn_claim);
        binding.btnClaim.setOnClickListener(v -> showClaimDialog());
    }

    private void loadReporterContact() {
        String authHeader = TokenManager.getAuthHeader(this);
        if (authHeader == null || TextUtils.isEmpty(itemId)) {
            Toast.makeText(this, R.string.msg_contact_load_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        binding.btnClaim.setEnabled(false);
        apiService.getLostItemOwnerContact(authHeader, itemId)
                .enqueue(new Callback<ContactResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ContactResponse> call,
                                           @NonNull Response<ContactResponse> response) {
                        binding.btnClaim.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            ContactInfoDialog.show(ItemDetailsActivity.this, response.body(),
                                    ItemDetailsActivity.this,
                                    callback -> {
                                        String header = TokenManager.getAuthHeader(
                                                ItemDetailsActivity.this);
                                        if (header == null || TextUtils.isEmpty(itemId)) {
                                            return;
                                        }
                                        apiService.getLostItemOwnerContact(header, itemId)
                                                .enqueue(callback);
                                    });
                        } else {
                            ConnectionToasts.showIfOnline(ItemDetailsActivity.this,
                                    R.string.msg_contact_load_failed, Toast.LENGTH_SHORT);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ContactResponse> call,
                                          @NonNull Throwable t) {
                        binding.btnClaim.setEnabled(true);
                        ConnectionToasts.showIfOnline(ItemDetailsActivity.this,
                                R.string.msg_contact_load_failed, Toast.LENGTH_SHORT);
                    }
                });
    }

    /**
     * @return {@code true} if this is the current user's report
     */
    private boolean isCurrentUserOwner() {
        if (TextUtils.isEmpty(ownerId)) {
            return false;
        }
        String currentUserId = TokenManager.getUserId(this);
        return !TextUtils.isEmpty(currentUserId) && ownerId.equals(currentUserId);
    }

    /**
     * Asks a small question before sending the claim. The box stays open if the answer is empty.
     */
    private void showClaimDialog() {
        // Custom view for the answer field
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_claim_verification, null);

        TextInputLayout inputLayout = dialogView.findViewById(R.id.input_layout_claim_answer);
        TextInputEditText editAnswer = dialogView.findViewById(R.id.edit_claim_answer);

        String questionText = !TextUtils.isEmpty(verificationQuestion)
                ? verificationQuestion
                : getString(R.string.claim_default_question);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.claim_dialog_title)
                .setMessage(getString(R.string.claim_dialog_message, questionText))
                .setView(dialogView)
                .setPositiveButton(R.string.btn_submit, null)  // we handle the click ourselves
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        // Stay open if the answer is empty
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String answer = editAnswer.getText() != null
                        ? editAnswer.getText().toString().trim() : "";

                if (TextUtils.isEmpty(answer)) {
                    inputLayout.setError(getString(R.string.msg_claim_answer_required));
                    editAnswer.requestFocus();
                    return;
                }

                // Close the dialog
                inputLayout.setError(null);
                dialog.dismiss();

                // Send the claim
                submitClaimToBackend(answer);
            });
        });

        dialog.show();
    }

    /**
     * Sends the claim to the server. The button is disabled while waiting.
     *
     * @param validationAnswer the user's answer
     */
    private void submitClaimToBackend(String validationAnswer) {
        // Need an item id
        if (TextUtils.isEmpty(itemId)) {
            Toast.makeText(this,
                    R.string.msg_item_id_missing,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Need to be signed in
        String authHeader = TokenManager.getAuthHeader(this);
        if (authHeader == null) {
            Toast.makeText(this,
                    R.string.msg_login_to_claim,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Avoid a second tap
        setClaimButtonLoading(true);

        // Send the request
        ClaimRequest request = new ClaimRequest(itemId, validationAnswer);

        apiService.submitClaim(authHeader, request).enqueue(new Callback<Claim>() {
            @Override
            public void onResponse(Call<Claim> call, Response<Claim> response) {
                setClaimButtonLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ItemDetailsActivity.this,
                            R.string.msg_claim_submitted,
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    handleClaimError(response.code());
                }
            }

            @Override
            public void onFailure(Call<Claim> call, Throwable t) {
                setClaimButtonLoading(false);
                ConnectionToasts.showIfOnline(ItemDetailsActivity.this,
                        getString(R.string.msg_network_error, t.getLocalizedMessage()),
                        Toast.LENGTH_LONG);
            }
        });
    }

    /**
     * Shows a short error based on the server status.
     *
     * @param statusCode the status from the server
     */
    private void handleClaimError(int statusCode) {
        String errorMsg;

        switch (statusCode) {
            case 400:
                errorMsg = getString(R.string.msg_claim_already_claimed);
                break;
            case 401:
                errorMsg = getString(R.string.msg_session_expired);
                break;
            case 404:
                errorMsg = getString(R.string.msg_item_not_found);
                break;
            default:
                if (statusCode >= 500) {
                    errorMsg = getString(R.string.msg_server_error);
                } else {
                    errorMsg = getString(R.string.msg_claim_submit_failed, statusCode);
                }
                break;
        }

        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }

    /**
     * Disables the button while the claim is sending.
     *
     * @param isLoading {@code true} while sending
     */
    private void setClaimButtonLoading(boolean isLoading) {
        claimSubmitting = isLoading;
        binding.btnClaim.setEnabled(!isLoading);
        if (isLoading) {
            binding.btnClaim.setText(R.string.btn_submitting);
        } else {
            setupBottomAction();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
