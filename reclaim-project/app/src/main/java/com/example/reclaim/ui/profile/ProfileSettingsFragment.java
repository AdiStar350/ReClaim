package com.example.reclaim.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.reclaim.R;
import com.example.reclaim.databinding.FragmentProfileSettingsBinding;
import com.example.reclaim.model.User;
import com.example.reclaim.network.DeleteAccountRequest;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Settings hub: profile summary, account actions, logout, and delete account.
 * Personal information is edited on {@link EditProfileActivity}.
 */
public class ProfileSettingsFragment extends Fragment {

    private FragmentProfileSettingsBinding binding;
    private ReClaimApiService apiService;
    private boolean showInitialProgress = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getApiService();
        binding.textAppVersion.setText(getString(R.string.app_version_label, "1.0"));

        View.OnClickListener openPersonalInfo = v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class));
        binding.cardProfile.setOnClickListener(openPersonalInfo);
        binding.rowPersonalInfo.setOnClickListener(openPersonalInfo);
        binding.rowPendingClaims.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PendingClaimsActivity.class)));
        binding.rowLogout.setOnClickListener(v -> logout());
        binding.rowDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) {
            return;
        }

        if (showInitialProgress) {
            binding.progress.setVisibility(View.VISIBLE);
        }
        apiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (binding == null || !isAdded()) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                showInitialProgress = false;
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    if (user.getName() != null) {
                        binding.textUserDisplayName.setText(user.getName());
                    }
                    if (user.getEmail() != null) {
                        binding.textUserDisplayEmail.setText(user.getEmail());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (binding == null || !isAdded()) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                showInitialProgress = false;
                Toast.makeText(requireContext(),
                        R.string.msg_profile_load_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        TokenManager.clearToken(requireContext());
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showDeleteAccountDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_account, null);
        TextInputLayout nameLayout = dialogView.findViewById(R.id.input_layout_confirm_name);
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.input_layout_confirm_password);
        TextInputEditText nameInput = dialogView.findViewById(R.id.edit_confirm_name);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.edit_confirm_password);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_account_title)
                .setView(dialogView)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_delete_account, (d, w) -> {
                    String name = nameInput.getText() != null
                            ? nameInput.getText().toString().trim() : "";
                    String password = passwordInput.getText() != null
                            ? passwordInput.getText().toString() : "";
                    if (TextUtils.isEmpty(name)) {
                        nameLayout.setError(getString(R.string.msg_name_required));
                        return;
                    }
                    if (TextUtils.isEmpty(password)) {
                        passwordLayout.setError(getString(R.string.msg_password_required));
                        return;
                    }
                    deleteAccount(name, password);
                })
                .show();
    }

    private void deleteAccount(String name, String password) {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) {
            return;
        }

        binding.progress.setVisibility(View.VISIBLE);
        apiService.deleteAccount(authHeader, new DeleteAccountRequest(name, password))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (binding == null || !isAdded()) {
                            return;
                        }
                        binding.progress.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            TokenManager.clearToken(requireContext());
                            Toast.makeText(requireContext(),
                                    R.string.msg_account_deleted, Toast.LENGTH_SHORT).show();
                            logout();
                        } else {
                            Toast.makeText(requireContext(),
                                    R.string.msg_delete_account_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (binding == null || !isAdded()) {
                            return;
                        }
                        binding.progress.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                R.string.msg_delete_account_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
