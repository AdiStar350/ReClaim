```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityItemDetailsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    apiService = RetrofitClient.getApiService();

    setupToolbar();
    populateFromIntent();
    setupBottomAction();
    AppStateEvents.observeNew(this, this::refreshFromServer);
}
```

What that function does: I show the details screen from extras, then I watch for later reloads.
What the function is responsible for: I am the details start.
Where it works: ItemNavigationHelper and AppNotifications start me. I inflate activity_item_details.xml.
Why I chose to write the code of the function the way its written: Extras paint the screen at once. The server refresh can correct them.
More: I use ActivityItemDetailsBinding and AppStateEvents.observeNew.

```java
@Override
protected void onResume() {
    super.onResume();
    refreshFromServer();
}
```

What that function does: I reload the item when I come back.
What the function is responsible for: I keep status and the claim button current.
Where it works: Android calls me. AppStateEvents also calls refreshFromServer.
Why I chose to write the code of the function the way its written: Returning from a found-report form should flip the button.
More: I call refreshFromServer.

```java
private void setupToolbar() {
    setSupportActionBar(binding.toolbarItemDetails);
    if (getSupportActionBar() != null) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }
    // Title sits below the photo, not on it.
    binding.collapsingToolbar.setTitleEnabled(false);
}
```

What that function does: I add a back arrow and I turn off the collapsing title.
What the function is responsible for: I am the details toolbar.
Where it works: I am called from onCreate. Back goes through onSupportNavigateUp.
Why I chose to write the code of the function the way its written: The title under the photo is large enough. A toolbar title would cover the image.
More: I use setSupportActionBar.

```java
private void populateFromIntent() {
    itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
    verificationQuestion = getIntent().getStringExtra(EXTRA_ITEM_VERIFICATION_QUESTION);
    ownerId = getIntent().getStringExtra(EXTRA_ITEM_OWNER_ID);
    itemType = getIntent().getStringExtra(EXTRA_ITEM_TYPE);
    hasUserAlreadyInteracted = getIntent().getBooleanExtra(
            EXTRA_HAS_USER_ALREADY_INTERACTED, false)
            || AppStateEvents.wasItemInteracted(itemId);

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
```

What that function does: I copy extras into fields and paint the screen.
What the function is responsible for: I am the first paint from the Intent.
Where it works: I am called from onCreate.
Why I chose to write the code of the function the way its written: Local interacted state covers a claim that just happened before the server list updates.
More: I use AppStateEvents.wasItemInteracted.

```java
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
```

What that function does: I fetch the item by id and refresh the screen.
What the function is responsible for: I am the live details reload.
Where it works: I am called from onResume and AppStateEvents.
Why I chose to write the code of the function the way its written: A failed call keeps the extras already shown.
More: I use getItemById.

```java
public void onResponse(@NonNull Call<Item> call, @NonNull Response<Item> response) {
    if (isFinishing()) {
        return;
    }
    if (response.isSuccessful() && response.body() != null) {
        applyItem(response.body());
    }
}
```

What that function does: I apply the server item if the screen is still open.
What the function is responsible for: I am the success path for getItemById.
Where it works: Retrofit calls me from refreshFromServer.
Why I chose to write the code of the function the way its written: isFinishing stops a late bind after back.
More: I call applyItem.

```java
public void onFailure(@NonNull Call<Item> call, @NonNull Throwable t) {
    // Keep what was already shown.
}
```

What that function does: I keep the extras if the fetch fails.
What the function is responsible for: I am the failure path for getItemById.
Where it works: Retrofit calls me from refreshFromServer.
Why I chose to write the code of the function the way its written: Details from the Intent are enough to read.
More: I leave the body empty.

```java
private void applyItem(@NonNull Item item) {
    if (!TextUtils.isEmpty(item.getId())) {
        itemId = item.getId();
    }
    verificationQuestion = item.getVerificationQuestion();
    ownerId = item.getOwnerId();
    itemType = item.getType();
    hasUserAlreadyInteracted = item.hasUserAlreadyInteracted()
            || AppStateEvents.wasItemInteracted(itemId);
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
```

What that function does: I copy the server item into fields and redraw.
What the function is responsible for: I am the bind from a live Item.
Where it works: I am called from refreshFromServer's onResponse.
Why I chose to write the code of the function the way its written: I also re-run setupBottomAction so the claim button matches the new status.
More: I OR local interacted with the server flag.

```java
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
```

What that function does: I fill title, chips, description, place, date, reporter, and photo.
What the function is responsible for: I am the details bind.
Where it works: I am called from populateFromIntent and applyItem.
Why I chose to write the code of the function the way its written: Placeholders keep empty extras readable. Status uses known strings when I can.
More: I use Glide for the item photo and reporter avatar.

```java
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
```

What that function does: I turn an ISO timestamp into a short date.
What the function is responsible for: I am the reported-at text.
Where it works: bindFields calls me.
Why I chose to write the code of the function the way its written: I trim fractions so SimpleDateFormat can parse. If parse fails I show the first ten characters.
More: I parse in UTC and format with Locale.getDefault.

```java
private void setupBottomAction() {
    if (claimSubmitting) {
        return;
    }
    if (isCurrentUserOwner()) {
        binding.bottomClaimContainer.setVisibility(View.GONE);
        return;
    }
    binding.bottomClaimContainer.setVisibility(View.VISIBLE);
    if (hasUserAlreadyInteracted) {
        showInteractedButton();
        return;
    }
    binding.btnClaim.setEnabled(true);
    binding.btnClaim.setAlpha(1f);
    if ("LOST".equalsIgnoreCase(itemType)) {
        binding.btnClaim.setText(R.string.btn_i_found_this);
        binding.btnClaim.setOnClickListener(v -> openFoundReportForLostItem());
        return;
    }
    binding.btnClaim.setText(R.string.btn_claim);
    binding.btnClaim.setOnClickListener(v -> showClaimDialog());
}
```

What that function does: I hide the bar for my own report, or I set claim / I found this.
What the function is responsible for: I am the bottom button.
Where it works: I am called from onCreate, applyItem, markInteractedLocally, and setClaimButtonLoading.
Why I chose to write the code of the function the way its written: Lost items open a found report. Found items ask the check question. I skip a redraw while a claim is sending.
More: I use isCurrentUserOwner.

```java
private void showInteractedButton() {
    boolean lost = "LOST".equalsIgnoreCase(itemType);
    binding.btnClaim.setText(lost
            ? R.string.btn_report_submitted
            : R.string.btn_claim_pending);
    binding.btnClaim.setEnabled(false);
    binding.btnClaim.setAlpha(0.5f);
    binding.btnClaim.setOnClickListener(null);
}
```

What that function does: I keep the button visible but not tappable after a claim or found report.
What the function is responsible for: I show that this item was already acted on.
Where it works: setupBottomAction calls me.
Why I chose to write the code of the function the way its written: Hiding the bar would look like the item vanished. Dimmed text is clearer.
More: I set OnClickListener to null.

```java
private void markInteractedLocally() {
    hasUserAlreadyInteracted = true;
    if (!TextUtils.isEmpty(itemId)) {
        AppStateEvents.markItemInteracted(itemId);
    }
    setupBottomAction();
}
```

What that function does: I mark this item acted on, then I redraw the button.
What the function is responsible for: I flip the button without waiting for a reload.
Where it works: I am called after a successful claim and from foundReportLauncher when ReportActivity returns RESULT_OK.
Why I chose to write the code of the function the way its written: AppStateEvents keeps the flag if the user opens details again before the server list updates.
More: I use AppStateEvents.markItemInteracted.

```java
private void openFoundReportForLostItem() {
    if (TextUtils.isEmpty(itemId)) {
        Toast.makeText(this, R.string.msg_item_id_missing, Toast.LENGTH_SHORT).show();
        return;
    }
    Intent intent = new Intent(this, ReportActivity.class);
    intent.putExtra(ReportActivity.EXTRA_LOCK_FOUND_TYPE, true);
    intent.putExtra(ReportActivity.EXTRA_TARGET_LOST_REPORT_ID, itemId);
    foundReportLauncher.launch(intent);
}
```

What that function does: I open a found-report form locked to this lost item.
What the function is responsible for: I am the I-found-this path.
Where it works: setupBottomAction sets this click. ReportActivity inflates activity_report.xml.
Why I chose to write the code of the function the way its written: The launcher waits for RESULT_OK so I can dim the button if they submitted.
More: I use EXTRA_LOCK_FOUND_TYPE and EXTRA_TARGET_LOST_REPORT_ID.

```java
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
```

What that function does: I load the lost-item owner's contact.
What the function is responsible for: I would show ContactInfoDialog for a lost report.
Where it works: I found no caller. The method sits in this class unused.
Why I chose to write the code of the function the way its written: I disable the bottom button while the call runs so a double tap does not start two loads.
More: I use getLostItemOwnerContact.

```java
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
```

What that function does: I open contact or toast, and I re-enable the button.
What the function is responsible for: I am the success path for getLostItemOwnerContact.
Where it works: Retrofit would call me from loadReporterContact.
Why I chose to write the code of the function the way its written: The refetch callback lets the dialog retry.
More: I use ContactInfoDialog.show.

```java
public void onFailure(@NonNull Call<ContactResponse> call,
                      @NonNull Throwable t) {
    binding.btnClaim.setEnabled(true);
    ConnectionToasts.showIfOnline(ItemDetailsActivity.this,
            R.string.msg_contact_load_failed, Toast.LENGTH_SHORT);
}
```

What that function does: I re-enable the button and toast if contact fails.
What the function is responsible for: I am the failure path for getLostItemOwnerContact.
Where it works: Retrofit would call me from loadReporterContact.
Why I chose to write the code of the function the way its written: The button must come back even on failure.
More: I use ConnectionToasts.showIfOnline.

```java
private boolean isCurrentUserOwner() {
    if (TextUtils.isEmpty(ownerId)) {
        return false;
    }
    String currentUserId = TokenManager.getUserId(this);
    return !TextUtils.isEmpty(currentUserId) && ownerId.equals(currentUserId);
}
```

What that function does: I check if this report belongs to the signed-in user.
What the function is responsible for: I hide claim actions on my own item.
Where it works: setupBottomAction calls me.
Why I chose to write the code of the function the way its written: Missing ownerId is treated as not mine so a guest extras path still shows a button.
More: I use TokenManager.getUserId.

```java
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
```

What that function does: I ask the check question and take an answer before sending a claim.
What the function is responsible for: I am the claim dialog.
Where it works: The found-item claim button calls me. I inflate dialog_claim_verification.xml.
Why I chose to write the code of the function the way its written: Positive is set to null first so an empty answer does not close the box.
More: I use MaterialAlertDialogBuilder and a default question if extras have none.

```java
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
            if (response.isSuccessful() && response.body() != null) {
                Toast.makeText(ItemDetailsActivity.this,
                        R.string.msg_claim_submitted,
                        Toast.LENGTH_LONG).show();
                claimSubmitting = false;
                markInteractedLocally();
            } else {
                setClaimButtonLoading(false);
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
```

What that function does: I send the claim with the answer and lock the button while waiting.
What the function is responsible for: I am the claim POST.
Where it works: showClaimDialog calls me after a non-empty answer.
Why I chose to write the code of the function the way its written: I check id and token first so I do not disable the button for a local miss.
More: I use ClaimRequest and submitClaim.

```java
public void onResponse(Call<Claim> call, Response<Claim> response) {
    if (response.isSuccessful() && response.body() != null) {
        Toast.makeText(ItemDetailsActivity.this,
                R.string.msg_claim_submitted,
                Toast.LENGTH_LONG).show();
        claimSubmitting = false;
        markInteractedLocally();
    } else {
        setClaimButtonLoading(false);
        handleClaimError(response.code());
    }
}
```

What that function does: I toast success and dim the button, or I show a status error.
What the function is responsible for: I am the success path for submitClaim.
Where it works: Retrofit calls me from submitClaimToBackend.
Why I chose to write the code of the function the way its written: markInteractedLocally redraws the pending text. claimSubmitting false lets that redraw run.
More: I call handleClaimError on a bad status.

```java
public void onFailure(Call<Claim> call, Throwable t) {
    setClaimButtonLoading(false);
    ConnectionToasts.showIfOnline(ItemDetailsActivity.this,
            getString(R.string.msg_network_error, t.getLocalizedMessage()),
            Toast.LENGTH_LONG);
}
```

What that function does: I unlock the button and toast a network miss.
What the function is responsible for: I am the failure path for submitClaim.
Where it works: Retrofit calls me from submitClaimToBackend.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast when the device is already offline.
More: I use ConnectionToasts.showIfOnline.

```java
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
```

What that function does: I map a claim status code to a short toast.
What the function is responsible for: I am claim error text.
Where it works: submitClaim's onResponse calls me.
Why I chose to write the code of the function the way its written: 400 is treated as already claimed, which matches the backend rule I saw.
More: I use a switch on the status.

```java
private void setClaimButtonLoading(boolean isLoading) {
    claimSubmitting = isLoading;
    binding.btnClaim.setEnabled(!isLoading);
    if (isLoading) {
        binding.btnClaim.setText(R.string.btn_submitting);
    } else {
        setupBottomAction();
    }
}
```

What that function does: I show Submitting, or I restore the real button.
What the function is responsible for: I stop a second claim tap.
Where it works: submitClaimToBackend calls me.
Why I chose to write the code of the function the way its written: Restoring through setupBottomAction puts the right label back after an error.
More: I set claimSubmitting so setupBottomAction does not fight a send in progress.

```java
@Override
public boolean onSupportNavigateUp() {
    getOnBackPressedDispatcher().onBackPressed();
    return true;
}
```

What that function does: I treat the toolbar back arrow as system back.
What the function is responsible for: I leave details.
Where it works: The toolbar up button calls me.
Why I chose to write the code of the function the way its written: OnBackPressedDispatcher keeps the same path as the system back key.
More: I return true so the event is consumed.
