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

What that function does: I show the sign-up form.
What the function is responsible for: I am sign-up step 1.
Where it works: LoginActivity starts me. I inflate activity_register.xml.
Why I chose to write the code of the function the way its written: I draw behind the system bars like login.
More: I use ActivityRegisterBinding and RetrofitClient.getApiService.

```java
private void setupClickListeners() {
    binding.btnRegister.setOnClickListener(v -> attemptRegistration());
    binding.btnGoLogin.setOnClickListener(v -> finish());

    android.view.View.OnClickListener pickPhoto = v -> galleryLauncher.launch("image/*");
    binding.cardAvatar.setOnClickListener(pickPhoto);
    binding.btnAddPhoto.setOnClickListener(pickPhoto);
}
```

What that function does: I wire register, back to login, and photo pick.
What the function is responsible for: I am the click setup for the sign-up form.
Where it works: I am called from onCreate. galleryLauncher uses GetContent for image/*.
Why I chose to write the code of the function the way its written: finish() returns to login because login started me. Avatar and add-photo share one picker.
More: I use ActivityResultContracts.GetContent.

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

What that function does: I upload the picked photo on a background thread and keep the URL.
What the function is responsible for: I start the profile photo upload as soon as a picture is picked.
Where it works: The gallery launcher calls me. ImageUploadService uses FOLDER_PROFILES.
Why I chose to write the code of the function the way its written: pendingId is a UUID path, not a user id, because the account does not exist yet. I restore the button on the UI thread.
More: I use Executors.newSingleThreadExecutor and ImageUploadService.uploadImage.

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

What that function does: I check the form, wait for a photo if needed, then I send the sign-up code.
What the function is responsible for: I am the register button path.
Where it works: I am called from btnRegister. Success goes to sendOtp.
Why I chose to write the code of the function the way its written: I retry a failed photo once so a flaky upload does not block sign-up forever. I do not send while uploadingImage is true.
More: I use Patterns.EMAIL_ADDRESS and ImageUploadService on retry.

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

What that function does: I POST the sign-up fields so the server can email a code, then I open verify.
What the function is responsible for: I am POST /api/auth/register/send-otp from this screen.
Where it works: I am called from attemptRegistration. Success starts VerifyOtpActivity with EXTRA_EMAIL.
Why I chose to write the code of the function the way its written: Mode defaults to register on VerifyOtpActivity, so I only pass email.
More: I use SendOtpRequest and apiService.sendRegistrationOtp.

```java
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
```

What that function does: I open verify-otp, or I map the HTTP error.
What the function is responsible for: I am the Retrofit success path for sendRegistrationOtp.
Where it works: Retrofit calls me from sendOtp.
Why I chose to write the code of the function the way its written: 409 and 400 need different toasts, so I use handleRegistrationError.
More: I use VerifyOtpActivity.EXTRA_EMAIL.

```java
public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
    setLoadingState(false);
    ConnectionToasts.showIfOnline(RegisterActivity.this,
            getString(R.string.msg_network_error, t.getLocalizedMessage()),
            Toast.LENGTH_LONG);
}
```

What that function does: I stop loading and toast a network error if the API is up.
What the function is responsible for: I am the Retrofit failure path for sendRegistrationOtp.
Where it works: Retrofit calls me from sendOtp.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline.

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

What that function does: I toast why sign-up could not start.
What the function is responsible for: I map HTTP codes from send-otp.
Where it works: I am called from sendOtp's onResponse.
Why I chose to write the code of the function the way its written: 409 is the usual taken-email case so it has its own string.
More: I use string resources for 409, 400, 422, and 5xx.

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

What that function does: I show the picked photo as a round avatar.
What the function is responsible for: I preview the picture before upload finishes.
Where it works: The gallery launcher calls me.
Why I chose to write the code of the function the way its written: I clear padding and tint so the placeholder icon does not sit on top of the photo.
More: I use Glide.circleCrop.

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

What that function does: I lock the form while send-otp or a retry upload runs.
What the function is responsible for: I stop a second submit.
Where it works: I am called from attemptRegistration and sendOtp.
Why I chose to write the code of the function the way its written: I also lock the photo picker so the URI does not change mid-call.
More: I toggle enabled flags and the button text.

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    uploadExecutor.shutdownNow();
}
```

What that function does: I stop the photo upload thread when the screen closes.
What the function is responsible for: I clean the executor so work does not outlive the activity.
Where it works: Android calls me when this activity is destroyed.
Why I chose to write the code of the function the way its written: shutdownNow is enough for a single-thread pool.
More: I use ExecutorService.shutdownNow.
