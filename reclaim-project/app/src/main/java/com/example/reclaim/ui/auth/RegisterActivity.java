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

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Registration screen (step 1 of the OTP sign-up flow).
 * <p>
 * Collects the user's details and an optional profile photo. The photo is
 * uploaded to Firebase Storage as soon as it's picked and its download URL is
 * held in local state. On submit, the details + photo URL are sent to
 * {@code POST /api/auth/register/send-otp}, which emails a 6-digit code; the
 * user is then taken to {@link VerifyOtpActivity} to finish registration.
 * </p>
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private ActivityRegisterBinding binding;
    private ReClaimApiService apiService;
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    /** Locally selected photo awaiting/behind upload (nullable). */
    private Uri selectedImageUri;

    /** Firebase Storage download URL for the picked photo (nullable). */
    private String uploadedProfileUrl;

    /** Stable per-selection id used as the Storage path segment. */
    private String pendingImageId;

    /** True while a background upload is in flight. */
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
     * Uploads the picked photo to Firebase Storage on a background thread and
     * stores the resulting URL. A random pending id is used for the path since
     * there is no user id yet.
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
            binding.inputLayoutFullName.setError("Please enter your full name");
            binding.editFullName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.inputLayoutEmail.setError("Please enter your email");
            binding.editEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.setError("Please enter a valid email address");
            binding.editEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            binding.inputLayoutPhone.setError("Please enter your phone number");
            binding.editPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPassword.setError("Please enter a password");
            binding.editPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            binding.inputLayoutPassword.setError("Password must be at least 6 characters");
            binding.editPassword.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.inputLayoutConfirmPassword.setError("Please confirm your password");
            binding.editConfirmPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.inputLayoutConfirmPassword.setError("Passwords do not match");
            binding.editConfirmPassword.requestFocus();
            return;
        }

        // Wait for an in-flight photo upload so its URL is included.
        if (uploadingImage) {
            Toast.makeText(this,
                    R.string.msg_uploading_photo_register, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        // If a photo was picked but its upload previously failed, retry once
        // before sending the OTP so the URL isn't silently dropped.
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
     * Calls the backend to generate + email an OTP, then navigates to the
     * verification screen carrying the email.
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
                Toast.makeText(RegisterActivity.this,
                        "Network error: " + t.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleRegistrationError(int statusCode) {
        String errorMsg;
        switch (statusCode) {
            case 409:
                errorMsg = "This email is already registered. Please sign in.";
                break;
            case 400:
                errorMsg = "Invalid registration data. Please check your inputs.";
                break;
            case 422:
                errorMsg = "Validation error. Please check your inputs.";
                break;
            default:
                if (statusCode >= 500) {
                    errorMsg = "Server error. Please try again later.";
                } else {
                    errorMsg = "Registration failed (Error " + statusCode + ")";
                }
                break;
        }
        Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
    }

    /**
     * Renders the picked image as a circular avatar, hiding the placeholder icon.
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
        binding.btnRegister.setText(isLoading ? "Creating Account…" : "Create Account");
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
