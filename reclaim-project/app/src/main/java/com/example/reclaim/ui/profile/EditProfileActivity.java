package com.example.reclaim.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.reclaim.R;
import com.example.reclaim.util.ConnectionToasts;
import com.example.reclaim.databinding.ActivityEditProfileBinding;
import com.example.reclaim.model.User;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.network.UpdateProfileRequest;
import com.example.reclaim.storage.ImageUploadService;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dedicated screen for editing the signed-in user's name, email, phone, and
 * profile picture.
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfile";

    private ActivityEditProfileBinding binding;
    private ReClaimApiService apiService;
    private User currentUser;
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    /** URL of the profile picture already stored for the user (nullable). */
    @Nullable
    private String existingProfilePictureUrl;

    /** Locally picked image awaiting upload on save (nullable). */
    @Nullable
    private Uri selectedImageUri;

    /**
     * Gallery picker. On selection, previews the image and marks it for upload.
     */
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    showAvatar(uri);
                }
            });

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

        // A newly picked photo must be uploaded to Storage before the profile
        // is saved; otherwise keep whatever URL the user already had.
        if (selectedImageUri != null) {
            uploadAvatarThenSave(authHeader, name, email, phone, selectedImageUri);
        } else {
            submitProfile(authHeader, name, email, phone, existingProfilePictureUrl);
        }
    }

    /**
     * Uploads the selected image to Firebase Storage on a background thread,
     * then submits the profile update with the resulting download URL.
     */
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

    private void setSaving(boolean saving) {
        binding.progress.setVisibility(saving ? View.VISIBLE : View.GONE);
        binding.btnSaveProfile.setEnabled(!saving);
        binding.btnChangePhoto.setEnabled(!saving);
        binding.cardAvatar.setEnabled(!saving);
    }

    /**
     * Renders the given image (content {@link Uri} or remote URL) as a circular
     * avatar, replacing the placeholder icon.
     *
     * @param model any model Glide can load (e.g. {@link Uri} or {@link String})
     */
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uploadExecutor.shutdownNow();
    }

    @NonNull
    private static String textOf(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}
