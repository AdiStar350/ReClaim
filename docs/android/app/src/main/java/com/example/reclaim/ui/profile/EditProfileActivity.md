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

What that function does: I show the edit form and load the current profile.
What the function is responsible for: I am the edit-profile screen start.
Where it works: ProfileSettingsFragment starts me. I inflate activity_edit_profile.xml.
Why I chose to write the code of the function the way its written: Avatar and change-photo share one gallery picker. I load after the clicks are wired.
More: I use ActivityEditProfileBinding and GetContent for image/*.

```java
private void loadProfile() {
    String authHeader = TokenManager.getAuthHeader(this);
    if (authHeader == null) {
        return;
    }

    setSaving(true);
    apiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
        @Override
        public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
            setSaving(false);
            if (response.isSuccessful() && response.body() != null) {
                currentUser = response.body();
                binding.editName.setText(currentUser.getName());
                binding.editEmail.setText(currentUser.getEmail());
                binding.editPhone.setText(currentUser.getPhoneNumber());
                existingProfilePictureUrl = currentUser.getProfilePictureUrl();
                if (!TextUtils.isEmpty(existingProfilePictureUrl)) {
                    showAvatar(existingProfilePictureUrl);
                }
            } else {
                Toast.makeText(EditProfileActivity.this,
                        R.string.msg_profile_load_failed, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
            setSaving(false);
            ConnectionToasts.showIfOnline(EditProfileActivity.this,
                    R.string.msg_profile_load_failed, Toast.LENGTH_SHORT);
        }
    });
}
```

What that function does: I fetch /users/me and fill the fields.
What the function is responsible for: I am the profile load.
Where it works: I am called from onCreate.
Why I chose to write the code of the function the way its written: I keep existingProfilePictureUrl so save can reuse it when no new photo is picked.
More: I use apiService.getCurrentUser and User getters.

```java
public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
    setSaving(false);
    if (response.isSuccessful() && response.body() != null) {
        currentUser = response.body();
        binding.editName.setText(currentUser.getName());
        binding.editEmail.setText(currentUser.getEmail());
        binding.editPhone.setText(currentUser.getPhoneNumber());
        existingProfilePictureUrl = currentUser.getProfilePictureUrl();
        if (!TextUtils.isEmpty(existingProfilePictureUrl)) {
            showAvatar(existingProfilePictureUrl);
        }
    } else {
        Toast.makeText(EditProfileActivity.this,
                R.string.msg_profile_load_failed, Toast.LENGTH_SHORT).show();
    }
}
```

What that function does: I fill the form from the user reply.
What the function is responsible for: I am the Retrofit success path for getCurrentUser.
Where it works: Retrofit calls me from loadProfile.
Why I chose to write the code of the function the way its written: I only show the avatar if a URL exists.
More: I use showAvatar with the URL string.

```java
public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
    setSaving(false);
    ConnectionToasts.showIfOnline(EditProfileActivity.this,
            R.string.msg_profile_load_failed, Toast.LENGTH_SHORT);
}
```

What that function does: I stop the spinner and toast if the API is up.
What the function is responsible for: I am the Retrofit failure path for getCurrentUser.
Where it works: Retrofit calls me from loadProfile.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline with a string resource.

```java
private void saveProfile() {
    if (currentUser == null) {
        return;
    }

    String name = textOf(binding.editName);
    String email = textOf(binding.editEmail);
    String phone = textOf(binding.editPhone);

    if (TextUtils.isEmpty(name)) {
        binding.inputLayoutName.setError(getString(R.string.msg_name_required));
        return;
    }
    binding.inputLayoutName.setError(null);

    String authHeader = TokenManager.getAuthHeader(this);
    if (authHeader == null) {
        return;
    }

    setSaving(true);

    // Upload a new photo first. Otherwise keep the one already saved.
    if (selectedImageUri != null) {
        uploadAvatarThenSave(authHeader, name, email, phone, selectedImageUri);
    } else {
        submitProfile(authHeader, name, email, phone, existingProfilePictureUrl);
    }
}
```

What that function does: I check the name, then I upload a new photo or save with the old URL.
What the function is responsible for: I am the save button path.
Where it works: I am called from btnSaveProfile.
Why I chose to write the code of the function the way its written: I wait for currentUser so I do not save before load finishes. A new Uri is uploaded before PUT.
More: I use textOf and TokenManager.getAuthHeader.

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
            Log.e(TAG, "Profile picture upload failed uri=" + imageUri, e);
            String detail = e.getMessage() != null
                    ? e.getMessage()
                    : getString(R.string.msg_image_upload_failed);
            runOnUiThread(() -> {
                setSaving(false);
                Toast.makeText(EditProfileActivity.this, detail, Toast.LENGTH_LONG).show();
            });
            return;
        }
        runOnUiThread(() -> submitProfile(authHeader, name, email, phone, uploadedUrl));
    });
}
```

What that function does: I upload the new photo, then I PUT the profile with that URL.
What the function is responsible for: I am the photo-then-save path.
Where it works: I am called from saveProfile when selectedImageUri is set.
Why I chose to write the code of the function the way its written: userId goes in the Storage path. I show the IOException message when I have one.
More: I use ImageUploadService.FOLDER_PROFILES and TokenManager.getUserId.

```java
private void submitProfile(@NonNull String authHeader, @NonNull String name,
                           @NonNull String email, @NonNull String phone,
                           @Nullable String profilePictureUrl) {
    UpdateProfileRequest request =
            new UpdateProfileRequest(name, email, phone, profilePictureUrl);
    apiService.updateCurrentUser(authHeader, request).enqueue(new Callback<User>() {
        @Override
        public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
            setSaving(false);
            if (response.isSuccessful() && response.body() != null) {
                Toast.makeText(EditProfileActivity.this,
                        R.string.msg_profile_updated, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this,
                        R.string.msg_profile_update_failed, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
            setSaving(false);
            Toast.makeText(EditProfileActivity.this,
                    R.string.msg_profile_update_failed, Toast.LENGTH_SHORT).show();
        }
    });
}
```

What that function does: I PUT name, email, phone, and picture URL, then I close on success.
What the function is responsible for: I am PUT /api/users/me from this screen.
Where it works: I am called from saveProfile and uploadAvatarThenSave.
Why I chose to write the code of the function the way its written: RESULT_OK lets the profile tab know it should reload. I finish so Back does not keep this form.
More: I use UpdateProfileRequest and apiService.updateCurrentUser.

```java
public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
    setSaving(false);
    if (response.isSuccessful() && response.body() != null) {
        Toast.makeText(EditProfileActivity.this,
                R.string.msg_profile_updated, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    } else {
        Toast.makeText(EditProfileActivity.this,
                R.string.msg_profile_update_failed, Toast.LENGTH_SHORT).show();
    }
}
```

What that function does: I close after a good save, or I toast a failure.
What the function is responsible for: I am the Retrofit success path for updateCurrentUser.
Where it works: Retrofit calls me from submitProfile.
Why I chose to write the code of the function the way its written: setResult(RESULT_OK) is for any caller that started me for a result.
More: I use Toast and finish.

```java
public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
    setSaving(false);
    Toast.makeText(EditProfileActivity.this,
            R.string.msg_profile_update_failed, Toast.LENGTH_SHORT).show();
}
```

What that function does: I stop saving and toast a failure.
What the function is responsible for: I am the Retrofit failure path for updateCurrentUser.
Where it works: Retrofit calls me from submitProfile.
Why I chose to write the code of the function the way its written: A simple failed toast is enough here.
More: I use msg_profile_update_failed.

```java
private void setSaving(boolean saving) {
    binding.progress.setVisibility(saving ? View.VISIBLE : View.GONE);
    binding.btnSaveProfile.setEnabled(!saving);
    binding.btnChangePhoto.setEnabled(!saving);
    binding.cardAvatar.setEnabled(!saving);
}
```

What that function does: I show a spinner and lock save and photo pick.
What the function is responsible for: I stop a second save while load or PUT runs.
Where it works: I am called from loadProfile, saveProfile, and the callbacks.
Why I chose to write the code of the function the way its written: I also lock the avatar so the Uri cannot change mid-upload.
More: I toggle progress visibility and enabled flags.

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

What that function does: I show a URL or a local Uri as a round avatar.
What the function is responsible for: I preview the photo.
Where it works: I am called from loadProfile with a URL and from the gallery launcher with a Uri.
Why I chose to write the code of the function the way its written: Object lets Glide take both String and Uri. I clear padding and tint like register.
More: I use Glide.circleCrop.

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    uploadExecutor.shutdownNow();
}
```

What that function does: I stop the upload thread when the screen closes.
What the function is responsible for: I clean the executor.
Where it works: Android calls me when this activity is destroyed.
Why I chose to write the code of the function the way its written: shutdownNow is enough for a single-thread pool.
More: I use ExecutorService.shutdownNow.

```java
@NonNull
private static String textOf(@Nullable TextInputEditText editText) {
    if (editText == null || editText.getText() == null) {
        return "";
    }
    return editText.getText().toString().trim();
}
```

What that function does: I read trimmed text from an edit field, or empty.
What the function is responsible for: I avoid null checks at each field.
Where it works: I am called from saveProfile.
Why I chose to write the code of the function the way its written: A helper keeps saveProfile shorter.
More: I use TextInputEditText.getText.
