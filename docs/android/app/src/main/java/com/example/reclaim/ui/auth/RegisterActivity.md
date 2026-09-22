# RegisterActivity.java

## Overview
This is step 1 of sign-up. An **Activity** is one full screen with its own logic and lifecycle (see `docs/GLOSSARY.md`). This screen collects the new user's name, email, phone, password, and an optional profile photo. When the form is valid it uploads the photo (if any) and asks the backend to email a one-time code (OTP), then opens `VerifyOtpActivity` for step 2.

## Concepts used here
- **Activity lifecycle** — `onCreate` builds the screen; `onDestroy` cleans up the background thread. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityRegisterBinding` exposes the views from `activity_register.xml`. See `docs/GLOSSARY.md`.
- **Activity Result API** — `registerForActivityResult` opens the photo gallery and receives the picked image back. See `docs/GLOSSARY.md`.
- **Background thread (`ExecutorService`)** — the photo upload runs off the main thread so the UI does not freeze; results are posted back with `runOnUiThread`.
- **Retrofit `Call` / `enqueue`** — sends the "email a code" request and reads the reply. See `docs/GLOSSARY.md`.
- **Glide** — loads the chosen photo into a round preview. See `docs/GLOSSARY.md`.
- **OTP** — a 6-digit code emailed to prove the address is real. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity` base class; `WindowCompat` for edge-to-edge; `ActivityResultLauncher`/`ActivityResultContracts.GetContent` for the gallery picker.
- **Android framework**: `Intent` opens the next screen; `Uri` points at the picked image; `TextUtils`/`Patterns` validate input; `Toast` shows short messages; `Log` records upload errors.
- **Glide** (`com.bumptech.glide.Glide`): loads the photo preview with `circleCrop()`.
- **Project classes**: `ActivityRegisterBinding` (layout), `RetrofitClient`/`ReClaimApiService` (API), `SendOtpRequest` (the JSON body), `ImageUploadService` (uploads the photo to storage and returns its URL), `ConnectionToasts` (network-aware error toast), `VerifyOtpActivity` (step 2).
- **Java standard**: `UUID` makes a random id for the photo path; `ExecutorService`/`Executors` run the upload on a single background thread; `IOException` is thrown if the upload fails.
- **Retrofit**: `Call`, `Callback`, `Response`.

## Fields
- `private static final String TAG = "RegisterActivity";` — label for log messages.
- `private ActivityRegisterBinding binding;` — typed access to all views in the layout.
- `private ReClaimApiService apiService;` — makes the backend calls.
- `private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();` — a one-thread background worker used to upload the photo without blocking the UI.
- `private Uri selectedImageUri;` — the photo the user picked (may still be uploading).
- `private String uploadedProfileUrl;` — the photo's URL after a successful upload, or null.
- `private String pendingImageId;` — the random id used as the photo's storage path.
- `private boolean uploadingImage;` — true while an upload is in progress; blocks submit until it finishes.
- `private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(...)` — opens the gallery; its callback stores the picked `Uri`, resets the previous URL, makes a new `pendingImageId`, shows the avatar, and kicks off `uploadProfileImage`. It must be created as a field (during construction) because the Activity Result API requires registration before the screen starts.

## Functions

### `private void setLoadingState(boolean isLoading)`
```java
private void setLoadingState(boolean isLoading) {
    binding.btnRegister.setEnabled(!isLoading);
    binding.btnRegister.setText(isLoading ? R.string.btn_creating_account : R.string.btn_register);
    binding.editFullName.setEnabled(!isLoading);
    binding.editEmail.setEnabled(!isLoading);
    binding.editPhone.setEnabled(!isLoading);
    binding.editPassword.setEnabled(!isLoading);
    binding.editConfirmPassword.setEnabled(!isLoading);
    binding.btnGoLogin.setEnabled(!isLoading);
    binding.cardAvatar.setEnabled(!isLoading);
    binding.btnAddPhoto.setEnabled(!isLoading);
}
```
- **What it does:** Enables or disables the whole form and changes the register button label while the app is busy.
- **Where it's called:** From `attemptRegistration` and from the `sendOtp` callbacks.
- **Why it's written this way:** Locking every input during the request prevents a double submit and signals that work is happening.
- **Step by step:**
  1. Toggle the register button and swap its text to "Creating account…" while loading.
  2. Toggle each text field, the go-to-login button, the avatar card, and the add-photo button together.

### `private void showAvatar(Uri uri)`
```java
private void showAvatar(@NonNull Uri uri) {
    binding.imageProfileAvatar.setPadding(0, 0, 0, 0);
    binding.imageProfileAvatar.setImageTintList(null);
    Glide.with(this)
            .load(uri)
            .circleCrop()
            .placeholder(R.drawable.ic_settings_person)
            .error(R.drawable.ic_settings_person)
            .into(binding.imageProfileAvatar);
}
```
- **What it does:** Shows the picked photo as a round avatar in the preview.
- **Where it's called:** From the `galleryLauncher` callback right after a photo is chosen.
- **Why it's written this way:** It removes the placeholder padding and tint so the real photo fills the circle, and uses Glide's `circleCrop()` for the round shape with a fallback icon.
- **Step by step:**
  1. Clear the padding and color tint used by the placeholder icon.
  2. Ask Glide to load the `uri`, crop it into a circle, and show it, using the person icon while loading or on error.

### `private void handleRegistrationError(int statusCode)`
```java
private void handleRegistrationError(int statusCode) {
    String errorMsg;
    switch (statusCode) {
        case 409:
            errorMsg = getString(R.string.msg_email_already_registered);
            break;
        case 400:
            errorMsg = getString(R.string.msg_invalid_registration);
            break;
        case 422:
            errorMsg = getString(R.string.msg_validation_error);
            break;
        case 500:
            // Server is up but the code email failed to send.
            errorMsg = getString(R.string.msg_otp_send_failed);
            break;
        default:
            if (statusCode >= 500) {
                errorMsg = getString(R.string.msg_server_error);
            } else {
                errorMsg = getString(R.string.msg_registration_failed, statusCode);
            }
            break;
    }
    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
}
```
- **What it does:** Turns an error status code from the "send code" request into a clear message.
- **Where it's called:** From the `sendOtp` `onResponse` when the response is not successful.
- **Why it's written this way:** Each known code (`409` already registered, `400` bad input, `422` validation, `500` email send failed) gets its own message; anything else falls back to a generic server or numbered error.
- **Step by step:**
  1. Pick a message based on the status code.
  2. For unknown codes ≥ 500 use "server error"; otherwise show a generic message including the code.
  3. Toast the chosen message.

### `private void sendOtp(String email, String password, String name, String phone, String profilePictureUrl)`
```java
private void sendOtp(String email, String password, String name,
                     String phone, String profilePictureUrl) {
    SendOtpRequest request = new SendOtpRequest(
            email, password, name, phone, profilePictureUrl);

    apiService.sendRegistrationOtp(request).enqueue(new Callback<Void>() {
        @Override
        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
            setLoadingState(false);
            if (response.isSuccessful()) {
                Toast.makeText(RegisterActivity.this,
                        R.string.msg_otp_sent, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, VerifyOtpActivity.class);
                intent.putExtra(VerifyOtpActivity.EXTRA_EMAIL, email);
                startActivity(intent);
            } else {
                handleRegistrationError(response.code());
            }
        }

        @Override
        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
            setLoadingState(false);
            ConnectionToasts.showIfOnline(RegisterActivity.this,
                    getString(R.string.msg_network_error, t.getLocalizedMessage()),
                    Toast.LENGTH_LONG);
        }
    });
}
```
- **What it does:** Sends all the collected details to the backend so it can email a verification code, then opens step 2.
- **Where it's called:** From `attemptRegistration` — either directly (no photo, or photo already uploaded) or after a retry upload succeeds.
- **Why it's written this way:** The whole registration payload is carried in the OTP request so the account can be created only after the code is confirmed on the next screen. The email is passed forward as an intent extra.
- **Step by step:**
  1. Build a `SendOtpRequest` with email, password, name, phone, and photo URL.
  2. `enqueue` `apiService.sendRegistrationOtp(...)`.
  3. In `onResponse`: stop loading; on success toast "code sent" and start `VerifyOtpActivity` with the email extra.
  4. On an error response, delegate to `handleRegistrationError(code)`.
  5. In `onFailure`: stop loading and show a network error only if the backend is online.

### `private void uploadProfileImage(Uri uri, String pendingId)`
```java
private void uploadProfileImage(@NonNull Uri uri, @NonNull String pendingId) {
    uploadingImage = true;
    binding.btnAddPhoto.setText(R.string.msg_uploading_photo_register);
    binding.btnAddPhoto.setEnabled(false);

    uploadExecutor.execute(() -> {
        String url = null;
        try {
            url = ImageUploadService.uploadImage(
                    getApplicationContext(), uri, pendingId,
                    ImageUploadService.FOLDER_PROFILES);
        } catch (IOException e) {
            Log.e(TAG, "Profile picture upload failed uri=" + uri, e);
        }
        final String uploaded = url;
        runOnUiThread(() -> {
            uploadingImage = false;
            uploadedProfileUrl = uploaded;
            if (binding == null) {
                return;
            }
            binding.btnAddPhoto.setEnabled(true);
            binding.btnAddPhoto.setText(R.string.register_add_photo);
            if (uploaded == null) {
                Toast.makeText(RegisterActivity.this,
                        R.string.msg_image_upload_failed, Toast.LENGTH_LONG).show();
            }
        });
    });
}
```
- **What it does:** Uploads the chosen photo to storage on a background thread and remembers its URL, so it is ready by the time the user submits.
- **Where it's called:** From the `galleryLauncher` callback, right after a photo is picked.
- **Why it's written this way:** Uploading early (as soon as the photo is chosen) means the submit step usually has the URL already. Upload is network/disk work, so it runs on `uploadExecutor` and the result is posted back with `runOnUiThread`. The `uploadingImage` flag lets `attemptRegistration` wait if needed.
- **Step by step:**
  1. Mark `uploadingImage = true`, change the button to "Uploading…", and disable it.
  2. On the background thread, call `ImageUploadService.uploadImage(...)` into the profiles folder; catch and log any `IOException`.
  3. Back on the UI thread, clear the uploading flag and store the URL (may be null).
  4. If the binding is gone, stop.
  5. Re-enable and relabel the button; if the upload failed (null URL), toast the failure.

### `private void attemptRegistration()`
```java
private void attemptRegistration() {
    binding.inputLayoutFullName.setError(null);
    binding.inputLayoutEmail.setError(null);
    binding.inputLayoutPhone.setError(null);
    binding.inputLayoutPassword.setError(null);
    binding.inputLayoutConfirmPassword.setError(null);

    String fullName = binding.editFullName.getText() != null
            ? binding.editFullName.getText().toString().trim() : "";
    String email = binding.editEmail.getText() != null
            ? binding.editEmail.getText().toString().trim() : "";
    String phone = binding.editPhone.getText() != null
            ? binding.editPhone.getText().toString().trim() : "";
    String password = binding.editPassword.getText() != null
            ? binding.editPassword.getText().toString().trim() : "";
    String confirmPassword = binding.editConfirmPassword.getText() != null
            ? binding.editConfirmPassword.getText().toString().trim() : "";

    if (TextUtils.isEmpty(fullName)) {
        binding.inputLayoutFullName.setError(getString(R.string.msg_enter_full_name));
        binding.editFullName.requestFocus();
        return;
    }
    if (TextUtils.isEmpty(email)) {
        binding.inputLayoutEmail.setError(getString(R.string.msg_enter_email));
        binding.editEmail.requestFocus();
        return;
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        binding.inputLayoutEmail.setError(getString(R.string.msg_enter_valid_email));
        binding.editEmail.requestFocus();
        return;
    }
    if (TextUtils.isEmpty(phone)) {
        binding.inputLayoutPhone.setError(getString(R.string.msg_enter_phone));
        binding.editPhone.requestFocus();
        return;
    }
    if (TextUtils.isEmpty(password)) {
        binding.inputLayoutPassword.setError(getString(R.string.msg_enter_password_generic));
        binding.editPassword.requestFocus();
        return;
    }
    if (password.length() < 6) {
        binding.inputLayoutPassword.setError(getString(R.string.msg_password_min_length));
        binding.editPassword.requestFocus();
        return;
    }
    if (TextUtils.isEmpty(confirmPassword)) {
        binding.inputLayoutConfirmPassword.setError(getString(R.string.msg_confirm_password));
        binding.editConfirmPassword.requestFocus();
        return;
    }
    if (!password.equals(confirmPassword)) {
        binding.inputLayoutConfirmPassword.setError(getString(R.string.msg_passwords_mismatch));
        binding.editConfirmPassword.requestFocus();
        return;
    }

    // Wait until the photo finishes uploading.
    if (uploadingImage) {
        Toast.makeText(this,
                R.string.msg_uploading_photo_register, Toast.LENGTH_SHORT).show();
        return;
    }

    setLoadingState(true);

    // Photo was picked but upload failed. Try once more before sending the code.
    if (selectedImageUri != null && uploadedProfileUrl == null) {
        final String fEmail = email, fPassword = password, fName = fullName, fPhone = phone;
        final String pendingId = pendingImageId != null
                ? pendingImageId : UUID.randomUUID().toString();
        uploadExecutor.execute(() -> {
            String url = null;
            try {
                url = ImageUploadService.uploadImage(
                        getApplicationContext(), selectedImageUri, pendingId,
                        ImageUploadService.FOLDER_PROFILES);
            } catch (IOException e) {
                Log.e(TAG, "Retry profile upload failed uri=" + selectedImageUri, e);
            }
            if (url == null) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    Toast.makeText(RegisterActivity.this,
                            R.string.msg_image_upload_failed, Toast.LENGTH_LONG).show();
                });
                return;
            }
            uploadedProfileUrl = url;
            runOnUiThread(() ->
                    sendOtp(fEmail, fPassword, fName, fPhone, uploadedProfileUrl));
        });
        return;
    }

    sendOtp(email, password, fullName, phone, uploadedProfileUrl);
}
```
- **What it does:** Validates every field, waits for or retries the photo upload, and then calls `sendOtp` to email the code.
- **Where it's called:** From the Register button, wired in `setupClickListeners`.
- **Why it's written this way:** Every field is checked locally, in order, so problems are caught before the network. If the photo is still uploading it asks the user to wait; if a photo was picked but its upload failed earlier, it retries once before sending, so a temporary glitch does not lose the photo.
- **Step by step:**
  1. Clear old field errors and read all five fields (trimmed).
  2. Run the checks in order (name, email present, email valid, phone, password present, password length, confirm present, passwords match); each failure sets an error, refocuses, and returns.
  3. If an upload is still running, toast "uploading" and stop.
  4. Lock the form with `setLoadingState(true)`.
  5. If a photo was picked but has no URL yet, retry the upload on the background thread; on failure toast and unlock, on success store the URL and call `sendOtp` from the UI thread.
  6. Otherwise call `sendOtp` immediately with whatever photo URL exists (possibly null).

### `private void setupClickListeners()`
```java
private void setupClickListeners() {
    binding.btnRegister.setOnClickListener(v -> attemptRegistration());
    binding.btnGoLogin.setOnClickListener(v -> finish());

    android.view.View.OnClickListener pickPhoto = v -> galleryLauncher.launch("image/*");
    binding.cardAvatar.setOnClickListener(pickPhoto);
    binding.btnAddPhoto.setOnClickListener(pickPhoto);
}
```
- **What it does:** Wires the register button, the "back to login" button, and the two ways to pick a photo.
- **Where it's called:** Once from `onCreate`.
- **Why it's written this way:** "Go to login" just calls `finish()` because login is the screen underneath, so closing returns to it. The avatar card and the add-photo button share one listener that launches the gallery for images.
- **Step by step:**
  1. Register button → `attemptRegistration()`.
  2. Go-to-login button → `finish()` this screen.
  3. Both the avatar card and add-photo button → launch the gallery with `image/*`.

### `protected void onCreate(Bundle savedInstanceState)`
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

    binding = ActivityRegisterBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    apiService = RetrofitClient.getApiService();

    setupClickListeners();
}
```
- **What it does:** Builds the screen and wires its buttons.
- **Where it's called:** Android calls it when the Activity is created.
- **Why it's written this way:** Standard setup: edge-to-edge window, inflate the layout with ViewBinding, get the API client once, then attach listeners.
- **Step by step:**
  1. Call `super.onCreate`.
  2. Draw behind the system bars.
  3. Inflate `activity_register.xml` and set it as content.
  4. Get the shared API service.
  5. Wire the click listeners.

### `protected void onDestroy()`
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    uploadExecutor.shutdownNow();
}
```
- **What it does:** Stops the background upload thread when the screen is closed.
- **Where it's called:** Android calls it as the Activity is destroyed (lifecycle end).
- **Why it's written this way:** `shutdownNow()` releases the thread so no upload keeps running after the screen is gone, avoiding leaks.
- **Step by step:**
  1. Call `super.onDestroy`.
  2. Shut down `uploadExecutor`.
