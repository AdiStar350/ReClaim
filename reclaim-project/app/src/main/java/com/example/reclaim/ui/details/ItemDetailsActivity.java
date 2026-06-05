package com.example.reclaim.ui.details;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.reclaim.R;
import com.example.reclaim.databinding.ActivityItemDetailsBinding;
import com.example.reclaim.model.Claim;
import com.example.reclaim.network.ClaimRequest;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Displays detailed information about a specific lost or found item.
 * <p>
 * Features a {@link com.google.android.material.appbar.CollapsingToolbarLayout}
 * with a header image loaded via Glide, item metadata (title, description,
 * location, date, status, category), and a "This is Mine!" claim button
 * that presents a verification dialog before submitting a claim to the
 * backend via Retrofit.
 * </p>
 *
 * <h3>Intent extras:</h3>
 * <ul>
 *   <li>{@code ITEM_ID} — the item's unique identifier (required for claim submission)</li>
 *   <li>{@code ITEM_TITLE} — the item's title</li>
 *   <li>{@code ITEM_DESCRIPTION} — the item's description</li>
 *   <li>{@code ITEM_LOCATION} — the item's location</li>
 *   <li>{@code ITEM_STATUS} — the item's status ("Lost" / "Found")</li>
 *   <li>{@code ITEM_CATEGORY} — the item's category</li>
 *   <li>{@code ITEM_IMAGE_URL} — the item's image URL</li>
 * </ul>
 */
public class ItemDetailsActivity extends AppCompatActivity {

    /** Intent extra key for the item's unique ID. */
    public static final String EXTRA_ITEM_ID = "ITEM_ID";
    public static final String EXTRA_ITEM_TITLE = "ITEM_TITLE";
    public static final String EXTRA_ITEM_DESCRIPTION = "ITEM_DESCRIPTION";
    public static final String EXTRA_ITEM_LOCATION = "ITEM_LOCATION";
    public static final String EXTRA_ITEM_STATUS = "ITEM_STATUS";
    public static final String EXTRA_ITEM_CATEGORY = "ITEM_CATEGORY";
    public static final String EXTRA_ITEM_IMAGE_URL = "ITEM_IMAGE_URL";
    public static final String EXTRA_ITEM_TYPE = "ITEM_TYPE";
    public static final String EXTRA_ITEM_VERIFICATION_QUESTION = "ITEM_VERIFICATION_QUESTION";

    private ActivityItemDetailsBinding binding;
    private ReClaimApiService apiService;

    /** The item's ID — required for claim submission. */
    private String itemId;
    private String verificationQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityItemDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getApiService();

        setupToolbar();
        populateFromIntent();
        setupClaimButton();
    }

    /**
     * Sets up the {@link com.google.android.material.appbar.CollapsingToolbarLayout}
     * and its inner toolbar with back navigation.
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbarItemDetails);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    /**
     * Populates the UI with data passed via Intent extras.
     * Falls back to placeholder text if extras are missing.
     */
    private void populateFromIntent() {
        itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        verificationQuestion = getIntent().getStringExtra(EXTRA_ITEM_VERIFICATION_QUESTION);

        String title = getIntent().getStringExtra(EXTRA_ITEM_TITLE);
        String description = getIntent().getStringExtra(EXTRA_ITEM_DESCRIPTION);
        String location = getIntent().getStringExtra(EXTRA_ITEM_LOCATION);
        String status = getIntent().getStringExtra(EXTRA_ITEM_STATUS);
        String category = getIntent().getStringExtra(EXTRA_ITEM_CATEGORY);
        String imageUrl = getIntent().getStringExtra(EXTRA_ITEM_IMAGE_URL);

        // Title
        binding.collapsingToolbar.setTitle(
                title != null ? title : "Item Details");
        binding.textItemTitle.setText(
                title != null ? title : "Item Details");

        // Description
        binding.textDescription.setText(
                description != null ? description : "No description provided.");

        // Location
        binding.textLocation.setText(
                location != null ? location : "Location not specified");

        // Status & Category chips
        if ("CLOSED".equalsIgnoreCase(status)) {
            binding.chipStatus.setText(R.string.status_closed);
        } else if ("OPEN".equalsIgnoreCase(status)) {
            binding.chipStatus.setText(R.string.status_open);
        } else {
            binding.chipStatus.setText(status != null ? status : "Unknown");
        }
        binding.chipCategory.setText(
                category != null ? category : "General");

        // Header image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.imageItem);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(binding.imageItem);
        }
    }

    /**
     * Configures the claim button to show a verification dialog when tapped.
     */
    private void setupClaimButton() {
        binding.btnClaim.setOnClickListener(v -> showClaimDialog());
    }

    /**
     * Presents a Material Design 3 dialog asking the user to answer
     * a verification question before submitting their ownership claim.
     * <p>
     * The dialog contains a {@link TextInputLayout} with an
     * {@link TextInputEditText} for the answer, a "Cancel" button,
     * and a "Submit" button. Validation is performed on submit —
     * the dialog stays open if the answer is empty.
     * </p>
     */
    private void showClaimDialog() {
        // Inflate a custom view with Material TextInputLayout for better UX
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_claim_verification, null);

        TextInputLayout inputLayout = dialogView.findViewById(R.id.input_layout_claim_answer);
        TextInputEditText editAnswer = dialogView.findViewById(R.id.edit_claim_answer);

        String questionText = !TextUtils.isEmpty(verificationQuestion)
                ? verificationQuestion
                : "Describe a unique identifying feature of this item.";

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Claim This Item")
                .setMessage("To verify ownership, please answer the following question:\n\n"
                        + questionText)
                .setView(dialogView)
                .setPositiveButton("Submit", null)  // set to null — we override below
                .setNegativeButton("Cancel", null)
                .create();

        // Override the positive button click to prevent auto-dismiss on empty input
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String answer = editAnswer.getText() != null
                        ? editAnswer.getText().toString().trim() : "";

                if (TextUtils.isEmpty(answer)) {
                    inputLayout.setError("Please provide a verification answer");
                    editAnswer.requestFocus();
                    return;
                }

                // Clear error and dismiss dialog
                inputLayout.setError(null);
                dialog.dismiss();

                // Submit the claim
                submitClaimToBackend(answer);
            });
        });

        dialog.show();
    }

    /**
     * Submits the ownership claim to the backend via Retrofit.
     * <p>
     * Disables the claim button and changes its text during the request.
     * On success, shows a Toast and finishes the activity. On failure,
     * re-enables the button and shows an error Toast.
     * </p>
     *
     * @param validationAnswer the user's answer to the verification question
     */
    private void submitClaimToBackend(String validationAnswer) {
        // Validate item ID
        if (TextUtils.isEmpty(itemId)) {
            Toast.makeText(this,
                    "Error: Item ID is missing",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the auth header
        String authHeader = TokenManager.getAuthHeader(this);
        if (authHeader == null) {
            Toast.makeText(this,
                    "Please log in to submit a claim",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable the claim button to prevent double-clicks
        setClaimButtonLoading(true);

        // Build and send the request
        ClaimRequest request = new ClaimRequest(itemId, validationAnswer);

        apiService.submitClaim(authHeader, request).enqueue(new Callback<Claim>() {
            @Override
            public void onResponse(Call<Claim> call, Response<Claim> response) {
                setClaimButtonLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ItemDetailsActivity.this,
                            "Claim submitted successfully! The reporter will be notified.",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    handleClaimError(response.code());
                }
            }

            @Override
            public void onFailure(Call<Claim> call, Throwable t) {
                setClaimButtonLoading(false);

                Toast.makeText(ItemDetailsActivity.this,
                        "Network error: " + t.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Displays a contextual error Toast based on the HTTP status code.
     *
     * @param statusCode the HTTP status code from the server response
     */
    private void handleClaimError(int statusCode) {
        String errorMsg;

        switch (statusCode) {
            case 400:
                errorMsg = "This item has already been claimed or you cannot claim your own item.";
                break;
            case 401:
                errorMsg = "Session expired. Please log in again.";
                break;
            case 404:
                errorMsg = "Item not found.";
                break;
            default:
                if (statusCode >= 500) {
                    errorMsg = "Server error. Please try again later.";
                } else {
                    errorMsg = "Failed to submit claim (Error " + statusCode + ")";
                }
                break;
        }

        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }

    /**
     * Toggles the claim button between loading and interactive states.
     *
     * @param isLoading {@code true} to show loading state
     */
    private void setClaimButtonLoading(boolean isLoading) {
        binding.btnClaim.setEnabled(!isLoading);
        binding.btnClaim.setText(isLoading ? "Submitting…" : "This is Mine!");
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
