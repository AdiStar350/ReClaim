# EditProfileActivity.java

## Overview
This is the edit-profile **Activity** (see `docs/GLOSSARY.md`). It loads the current user, lets them change name/email/phone and pick a new photo from the gallery, optionally uploads the photo to Firebase Storage, then saves the profile through the backend API.

## Concepts used here
- **Activity** — full edit screen. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityEditProfileBinding`. See `docs/GLOSSARY.md`.
- **Activity Result API** — `ActivityResultLauncher` + `GetContent` opens the gallery.
- **Retrofit Call / Callback** — get/update current user. See `docs/GLOSSARY.md`.
- **Firebase Storage (via ImageUploadService)** — uploads profile pictures. See `docs/GLOSSARY.md`.
- **Glide** — shows the round avatar. See `docs/GLOSSARY.md`.
- **ExecutorService** — runs the image upload off the UI thread.

## Libraries and imports
- **AndroidX / AppCompat**: AppCompatActivity, Activity Result contracts.
- **Glide**: circle-cropped avatar preview.
- **Material**: TextInputEditText for fields.
- **Retrofit**: Call, Callback, Response.
- **Project**: `User`, `UpdateProfileRequest`, `TokenManager`, `RetrofitClient`, `ImageUploadService`, `ConnectionToasts`.

## Fields
- `private ActivityEditProfileBinding binding` — edit-profile views.
- `private ReClaimApiService apiService` — API client.
- `private User currentUser` — last loaded user (needed before save).
- `private final ExecutorService uploadExecutor` — single background thread for uploads.
- `private String existingProfilePictureUrl` — URL already saved for this user.
- `private Uri selectedImageUri` — new gallery photo waiting to upload on save.
- `private final ActivityResultLauncher<String> galleryLauncher` — picks `image/*` and previews it.

## Functions

### `private static String textOf(TextInputEditText editText)`
```java
@NonNull
private static String textOf(@Nullable TextInputEditText editText) {
    if (editText == null || editText.getText() == null) {
        return "";
    }
    return editText.getText().toString().trim();
}
```
- **What it does:** Reads trimmed text from a field, or `""` if null.
- **Where it's called:** From `saveProfile` for name, email, and phone.
- **Why it's written this way:** Avoids repeating null checks on every field.
- **Step by step:**
  1. If edit text or its text is null, return empty string.
  2. Otherwise return trimmed `toString()`.

### `private void setSaving(boolean saving)`
```java
private void setSaving(boolean saving) {
    binding.progress.setVisibility(saving ? View.VISIBLE : View.GONE);
    binding.btnSaveProfile.setEnabled(!saving);
    binding.btnChangePhoto.setEnabled(!saving);
    binding.cardAvatar.setEnabled(!saving);
}
```
- **What it does:** Shows/hides progress and disables save/photo controls while a network or upload operation runs.
- **Where it's called:** From `loadProfile`, `saveProfile`, `uploadAvatarThenSave`, and `submitProfile` callbacks.
- **Why it's written this way:** Blocks double-save and photo changes mid-request.
- **Step by step:**
  1. Toggle progress visibility.
  2. Enable/disable save button, change-photo button, and avatar card.

### `private void showAvatar(Object model)`
```java
private void showAvatar(@NonNull Object model) {
    binding.imageProfileAvatar.setPadding(0, 0, 0, 0);
    binding.imageProfileAvatar.setImageTintList(null);
    Glide.with(this)
            .load(model)
            .circleCrop()
            .placeholder(R.drawable.ic_settings_person)
            .error(R.drawable.ic_settings_person)
            .into(binding.imageProfileAvatar);
}
```
- **What it does:** Clears default icon padding/tint and loads a round avatar from a URL string or Uri.
- **Where it's called:** From the gallery launcher callback and from `loadProfile` when an existing picture URL exists.
- **Why it's written this way:** Accepting `Object` lets Glide load either a remote URL or a local Uri the same way.
- **Step by step:**
  1. Zero padding and clear image tint.
  2. Glide load with circle crop, placeholder, and error icon.

### `private void loadProfile()`
```java
private void loadProfile() {
    String authHeader = TokenManager.getAuthHeader(this);
    if (authHeader == null) {
        return;
    }

    setSaving(true);
    apiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
        // onResponse: fill fields + avatar; onFailure: ConnectionToasts
    });
}
```
- **What it does:** Loads the signed-in user and fills the form (and avatar if a picture URL exists).
- **Where it's called:** From `onCreate`.
- **Why it's written this way:** Uses the same “saving” progress UI while loading so the form is not editable mid-fetch.
- **Step by step:**
  1. Require auth header.
  2. `setSaving(true)` and enqueue `getCurrentUser`.
  3. On success: store `currentUser`, set name/email/phone, remember picture URL, maybe `showAvatar`.
  4. On failure: toast via `ConnectionToasts`; always `setSaving(false)` when done.

### `private void submitProfile(String authHeader, String name, String email, String phone, String profilePictureUrl)`
```java
private void submitProfile(@NonNull String authHeader, @NonNull String name,
                           @NonNull String email, @NonNull String phone,
                           @Nullable String profilePictureUrl) {
    UpdateProfileRequest request =
            new UpdateProfileRequest(name, email, phone, profilePictureUrl);
    apiService.updateCurrentUser(authHeader, request).enqueue(new Callback<User>() {
        // success: toast, RESULT_OK, finish; else toast failure
    });
}
```
- **What it does:** POSTs the profile update DTO to the backend and closes with `RESULT_OK` on success.
- **Where it's called:** From `saveProfile` (no new photo) and from `uploadAvatarThenSave` after upload (on the UI thread).
- **Why it's written this way:** Keeps the HTTP update in one place whether the picture URL is old or freshly uploaded.
- **Step by step:**
  1. Build `UpdateProfileRequest`.
  2. Enqueue `updateCurrentUser`.
  3. On success: toast, `setResult(RESULT_OK)`, `finish`.
  4. On error/failure: toast update-failed and `setSaving(false)`.

### `private void uploadAvatarThenSave(...)`
```java
private void uploadAvatarThenSave(@NonNull String authHeader, @NonNull String name,
                                  @NonNull String email, @NonNull String phone,
                                  @NonNull Uri imageUri) {
    String userId = TokenManager.getUserId(this);
    if (userId == null) {
        setSaving(false);
        Toast.makeText(this, R.string.msg_profile_update_failed, Toast.LENGTH_SHORT).show();
        return;
    }

    uploadExecutor.execute(() -> {
        String uploadedUrl;
        try {
            uploadedUrl = ImageUploadService.uploadImage(
                    getApplicationContext(), imageUri, userId,
                    ImageUploadService.FOLDER_PROFILES);
        } catch (IOException e) {
            // log, toast detail on UI thread, return
            return;
        }
        runOnUiThread(() -> submitProfile(authHeader, name, email, phone, uploadedUrl));
    });
}
```
- **What it does:** Uploads the picked image on a background thread, then calls `submitProfile` with the new URL on the UI thread.
- **Where it's called:** From `saveProfile` when `selectedImageUri` is non-null.
- **Why it's written this way:** Firebase upload can block; running it on `uploadExecutor` keeps the UI responsive. Failure surfaces the exception message when present.
- **Step by step:**
  1. Require user id from TokenManager.
  2. On the executor, call `ImageUploadService.uploadImage` into `FOLDER_PROFILES`.
  3. On IOException: log, toast on UI, stop.
  4. On success: `runOnUiThread` → `submitProfile` with `uploadedUrl`.

### `private void saveProfile()`
```java
private void saveProfile() {
    if (currentUser == null) {
        return;
    }
    // read name/email/phone; require name; then upload or submit
}
```
- **What it does:** Validates name, then either uploads a new photo first or submits with the existing picture URL.
- **Where it's called:** Save button click in `onCreate`.
- **Why it's written this way:** Refuses to save before the initial load finished (`currentUser == null`). Photo upload happens only when the user picked a new Uri.
- **Step by step:**
  1. Abort if `currentUser` is null.
  2. Read fields; if name empty, set input error and return.
  3. Clear name error; require auth header; `setSaving(true)`.
  4. If `selectedImageUri != null`, `uploadAvatarThenSave`; else `submitProfile` with `existingProfilePictureUrl`.

### `protected void onCreate(Bundle savedInstanceState)`
```java
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    apiService = RetrofitClient.getApiService();
    binding.toolbarEditProfile.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    binding.btnSaveProfile.setOnClickListener(v -> saveProfile());

    View.OnClickListener pickPhoto = v -> galleryLauncher.launch("image/*");
    binding.cardAvatar.setOnClickListener(pickPhoto);
    binding.btnChangePhoto.setOnClickListener(pickPhoto);

    loadProfile();
}
```
- **What it does:** Inflates UI, wires back/save/photo pickers, loads profile.
- **Where it's called:** By Android when opened from `ProfileSettingsFragment` (Intent to this Activity; also listed in the manifest).
- **Why it's written this way:** Avatar card and “change photo” share one listener. Back uses the OnBackPressedDispatcher.
- **Step by step:**
  1. Inflate binding; get API service.
  2. Wire toolbar back and save.
  3. Wire photo pickers to `galleryLauncher.launch("image/*")`.
  4. Call `loadProfile()`.

### `protected void onDestroy()`
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    uploadExecutor.shutdownNow();
}
```
- **What it does:** Stops the upload executor when the screen is destroyed.
- **Where it's called:** By the Activity lifecycle.
- **Why it's written this way:** Avoids leaving a background thread after the screen is gone.
- **Step by step:**
  1. `super.onDestroy()`.
  2. `uploadExecutor.shutdownNow()`.
