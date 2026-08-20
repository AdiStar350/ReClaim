package com.example.reclaim.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.reclaim.R;
import com.example.reclaim.databinding.ActivityEditProfileBinding;
import com.example.reclaim.model.User;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.network.UpdateProfileRequest;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dedicated screen for editing the signed-in user's name, email, and phone.
 */
public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private ReClaimApiService apiService;
    private User currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getApiService();
        binding.toolbarEditProfile.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
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
                } else {
                    Toast.makeText(EditProfileActivity.this,
                            R.string.msg_profile_load_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                setSaving(false);
                Toast.makeText(EditProfileActivity.this,
                        R.string.msg_profile_load_failed, Toast.LENGTH_SHORT).show();
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
        UpdateProfileRequest request = new UpdateProfileRequest(name, email, phone);
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
    }

    @NonNull
    private static String textOf(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}
