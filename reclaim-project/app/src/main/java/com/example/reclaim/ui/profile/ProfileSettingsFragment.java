package com.example.reclaim.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.reclaim.R;
import com.example.reclaim.util.ConnectionToasts;
import com.example.reclaim.data.AppDataRefresh;
import com.example.reclaim.databinding.FragmentProfileSettingsBinding;
import com.example.reclaim.model.User;
import com.example.reclaim.network.DeleteAccountRequest;
import com.example.reclaim.network.ForgotPasswordRequest;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.ui.auth.LoginActivity;
import com.example.reclaim.ui.auth.VerifyOtpActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AlertDialog;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Settings: profile summary, account actions, logout, and delete.
 * Personal info is edited on {@link EditProfileActivity}.
 */
public class ProfileSettingsFragment extends Fragment {

    private FragmentProfileSettingsBinding binding;
    private ReClaimApiService apiService;
    private boolean showInitialProgress = true;
    private String currentEmail;

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

        // Opens the screen where the user edits their details.
        binding.cardProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        binding.rowReportHistory.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ReportHistoryActivity.class)));
        binding.rowNotifications.setOnClickListener(v -> openNotificationSettings());
        binding.rowPasswordUpdate.setOnClickListener(v -> startPasswordUpdate());
        binding.rowLogout.setOnClickListener(v -> logout());
        binding.rowDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
        AppDataRefresh.observeNew(getViewLifecycleOwner(), this::loadProfile);
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
                        currentEmail = user.getEmail();
                        binding.textUserDisplayEmail.setText(user.getEmail());
                    }
                    loadAvatar(user.getProfilePictureUrl());
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (binding == null || !isAdded()) {
                    return;
                }
                binding.progress.setVisibility(View.GONE);
                showInitialProgress = false;
                ConnectionToasts.showIfOnline(requireContext(),
                        R.string.msg_profile_load_failed, Toast.LENGTH_SHORT);
            }
        });
    }

    private void loadAvatar(@Nullable String url) {
        if (binding == null || url == null || url.isEmpty()) {
            return;
        }
        binding.imageSettingsAvatar.setPadding(0, 0, 0, 0);
        binding.imageSettingsAvatar.setImageTintList(null);
        Glide.with(this)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_settings_person)
                .error(R.drawable.ic_settings_person)
                .into(binding.imageSettingsAvatar);
    }

    /**
     * Opens this app's notification settings.
     */
    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        try {
            startActivity(intent);
        } catch (Exception e) {
            // Fall back to the app details page.
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + requireContext().getPackageName())));
        }
    }

    /**
     * Starts the same reset-password flow as login, with the user's email filled in.
     */
    private void startPasswordUpdate() {
        if (TextUtils.isEmpty(currentEmail)) {
            Toast.makeText(requireContext(),
                    R.string.msg_profile_load_failed, Toast.LENGTH_SHORT).show();
            loadProfile();
            return;
        }

        final String email = currentEmail;
        binding.progress.setVisibility(View.VISIBLE);
        apiService.sendPasswordResetOtp(new ForgotPasswordRequest(email))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (binding == null || !isAdded()) {
                            return;
                        }
                        binding.progress.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    R.string.msg_reset_code_sent, Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(requireContext(),
                                    VerifyOtpActivity.class);
                            intent.putExtra(VerifyOtpActivity.EXTRA_EMAIL, email);
                            intent.putExtra(VerifyOtpActivity.EXTRA_MODE,
                                    VerifyOtpActivity.MODE_RESET);
                            startActivity(intent);
                            return;
                        }
                        int message;
                        if (response.code() == 404 || response.code() == 400) {
                            message = R.string.msg_no_account_for_email;
                        } else if (response.code() == 500) {
                            // Server reached but the email did not go out.
                            message = R.string.msg_otp_send_failed;
                        } else {
                            message = R.string.error_generic;
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (binding == null || !isAdded()) {
                            return;
                        }
                        binding.progress.setVisibility(View.GONE);
                        ConnectionToasts.showIfOnline(requireContext(),
                                getString(R.string.msg_network_error, t.getLocalizedMessage()),
                                Toast.LENGTH_LONG);
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
        TextInputEditText nameInput = dialogView.findViewById(R.id.edit_confirm_name);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.edit_confirm_password);

        // Positive click is handled in the show listener so errors keep the dialog open.
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_account_title)
                .setView(dialogView)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_delete_account, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = nameInput.getText() != null
                            ? nameInput.getText().toString().trim() : "";
                    String password = passwordInput.getText() != null
                            ? passwordInput.getText().toString() : "";
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(requireContext(),
                                R.string.msg_delete_account_name_required,
                                Toast.LENGTH_SHORT).show();
                        nameInput.requestFocus();
                        return;
                    }
                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(requireContext(),
                                R.string.msg_delete_account_password_required,
                                Toast.LENGTH_SHORT).show();
                        passwordInput.requestFocus();
                        return;
                    }
                    deleteAccount(dialog, passwordInput, name, password);
                }));
        dialog.show();
    }

    private void deleteAccount(AlertDialog dialog, TextInputEditText passwordInput,
                               String name, String password) {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) {
            return;
        }

        setDeleteDialogBusy(dialog, true);
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
                            dialog.dismiss();
                            TokenManager.clearToken(requireContext());
                            Toast.makeText(requireContext(),
                                    R.string.msg_account_deleted, Toast.LENGTH_SHORT).show();
                            logout();
                            return;
                        }
                        setDeleteDialogBusy(dialog, false);
                        int message;
                        if (response.code() == 403) {
                            // Name or password did not match. Let the user try again.
                            message = R.string.msg_delete_account_wrong_credentials;
                            passwordInput.setText("");
                            passwordInput.requestFocus();
                        } else if (response.code() == 401) {
                            message = R.string.msg_session_expired;
                        } else {
                            message = R.string.msg_delete_account_try_again;
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (binding == null || !isAdded()) {
                            return;
                        }
                        binding.progress.setVisibility(View.GONE);
                        setDeleteDialogBusy(dialog, false);
                        Toast.makeText(requireContext(),
                                R.string.msg_delete_account_no_connection,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Blocks the dialog buttons while the delete request is running. */
    private void setDeleteDialogBusy(AlertDialog dialog, boolean busy) {
        dialog.setCancelable(!busy);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!busy);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!busy);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
