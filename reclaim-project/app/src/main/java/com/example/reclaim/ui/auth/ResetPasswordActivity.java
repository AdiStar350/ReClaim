package com.example.reclaim.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.reclaim.R;
import com.example.reclaim.databinding.ActivityResetPasswordBinding;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.ResetPasswordRequest;
import com.example.reclaim.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Final step of the forgot-password flow: choose and submit a new password.
 * <p>
 * Reached from {@link VerifyOtpActivity} in reset mode after the OTP is
 * validated. Uses the opaque reset token (proof of email ownership) together
 * with the new password to call {@code POST /api/auth/forgot-password/reset};
 * on success the user is returned to {@link LoginActivity} to sign in.
 * </p>
 */
public class ResetPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_RESET_TOKEN = "extra_reset_token";

    private ActivityResetPasswordBinding binding;
    private ReClaimApiService apiService;
    private String email;
    private String resetToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getApiService();
        email = getIntent().getStringExtra(EXTRA_EMAIL);
        resetToken = getIntent().getStringExtra(EXTRA_RESET_TOKEN);

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(resetToken)) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.toolbarReset.setNavigationOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnUpdatePassword.setOnClickListener(v -> attemptReset());
    }

    private void attemptReset() {
        binding.inputLayoutNewPassword.setError(null);
        binding.inputLayoutConfirmNewPassword.setError(null);

        String password = binding.editNewPassword.getText() != null
                ? binding.editNewPassword.getText().toString().trim() : "";
        String confirm = binding.editConfirmNewPassword.getText() != null
                ? binding.editConfirmNewPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutNewPassword.setError("Please enter a password");
            binding.editNewPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            binding.inputLayoutNewPassword.setError("Password must be at least 6 characters");
            binding.editNewPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            binding.inputLayoutConfirmNewPassword.setError("Passwords do not match");
            binding.editConfirmNewPassword.requestFocus();
            return;
        }

        setLoading(true);
        apiService.resetPassword(new ResetPasswordRequest(email, resetToken, password))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(ResetPasswordActivity.this,
                                    R.string.msg_password_updated, Toast.LENGTH_LONG).show();
                            navigateToLogin();
                        } else {
                            binding.inputLayoutNewPassword.setError(
                                    getString(R.string.msg_reset_invalid));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        setLoading(false);
                        Toast.makeText(ResetPasswordActivity.this,
                                "Network error: " + t.getLocalizedMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnUpdatePassword.setEnabled(!loading);
        binding.editNewPassword.setEnabled(!loading);
        binding.editConfirmNewPassword.setEnabled(!loading);
        binding.btnUpdatePassword.setText(loading
                ? "Updating…" : getString(R.string.btn_update_password));
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
