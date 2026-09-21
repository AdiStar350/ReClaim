package com.example.reclaim.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.reclaim.R;
import com.example.reclaim.databinding.ActivityRegisterBinding;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.SendOtpRequest;
import com.example.reclaim.storage.ImageUploadService;
import com.example.reclaim.util.ConnectionToasts;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Sign-up step 1. Collects details and an optional photo, then sends a code by email.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private ActivityRegisterBinding binding;
    private ReClaimApiService apiService;
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    /** Photo the user picked (may still be uploading). */
    private Uri selectedImageUri;

    /** Photo url after upload, if any. */
    private String uploadedProfileUrl;

    /** Id used for the photo path. */
    private String pendingImageId;

    /** True while the photo is uploading. */
    private boolean uploadingImage;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    uploadedProfileUrl = null;
                    pendingImageId = UUID.randomUUID().toString();
                    showAvatar(uri);
                    uploadProfileImage(uri, pendingImageId);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getApiService();

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> attemptRegistration());
        binding.btnGoLogin.setOnClickListener(v -> finish());

        android.view.View.OnClickListener pickPhoto = v -> galleryLauncher.launch("image/*");
        binding.cardAvatar.setOnClickListener(pickPhoto);
        binding.btnAddPhoto.setOnClickListener(pickPhoto);
    }

    /**
     * Uploads the photo in the background and keeps the url.
     */
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

    /**
     * Asks the server to email a code, then opens the next screen.
     */
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

    /**
     * Shows the picked photo as a round avatar.
     */
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uploadExecutor.shutdownNow();
    }
}
