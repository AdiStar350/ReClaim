package com.example.reclaim.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.reclaim.R;
import com.example.reclaim.databinding.ActivityLoginBinding;
import com.example.reclaim.network.ForgotPasswordRequest;
import com.example.reclaim.network.LoginRequest;
import com.example.reclaim.network.LoginResponse;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.ui.dashboard.DashboardActivity;
import com.example.reclaim.util.ConnectionToasts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Sign-in screen. On success it saves the token and opens the home screen.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ReClaimApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Draw behind the system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // API client
        apiService = RetrofitClient.getApiService();

        // Already signed in
        if (TokenManager.hasToken(this)) {
            navigateToDashboard();
            return;
        }

        setupClickListeners();
    }

    /**
     * Sets the button clicks.
     */
    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.btnGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        binding.btnForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    /**
     * Asks for an email, then sends a reset code.
     */
    private void showForgotPasswordDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_forgot_password, null);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.input_layout_reset_email);
        TextInputEditText emailInput = dialogView.findViewById(R.id.edit_reset_email);

        String typedOnLogin = binding.editEmail.getText() != null
                ? binding.editEmail.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(typedOnLogin)) {
            emailInput.setText(typedOnLogin);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.forgot_password_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.btn_send_reset_code, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button send = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            send.setOnClickListener(v -> {
                emailLayout.setError(null);
                String email = emailInput.getText() != null
                        ? emailInput.getText().toString().trim() : "";
                if (TextUtils.isEmpty(email)
                        || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailLayout.setError(getString(R.string.msg_email_required_reset));
                    emailInput.requestFocus();
                    return;
                }
                sendPasswordResetOtp(email, dialog, send);
            });
        });
        dialog.show();
    }

    private void sendPasswordResetOtp(@NonNull String email,
                                      @NonNull AlertDialog dialog,
                                      @NonNull Button sendButton) {
        sendButton.setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
        apiService.sendPasswordResetOtp(new ForgotPasswordRequest(email))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> response) {
                        if (isFinishing()) {
                            return;
                        }
                        sendButton.setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                        if (response.isSuccessful()) {
                            dialog.dismiss();
                            Toast.makeText(LoginActivity.this,
                                    R.string.msg_reset_code_sent, Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(LoginActivity.this,
                                    VerifyOtpActivity.class);
                            intent.putExtra(VerifyOtpActivity.EXTRA_EMAIL, email);
                            intent.putExtra(VerifyOtpActivity.EXTRA_MODE,
                                    VerifyOtpActivity.MODE_RESET);
                            startActivity(intent);
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    R.string.error_generic, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        if (isFinishing()) {
                            return;
                        }
                        sendButton.setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                        ConnectionToasts.showIfOnline(LoginActivity.this,
                                getString(R.string.msg_network_error, t.getLocalizedMessage()),
                                Toast.LENGTH_LONG);
                    }
                });
    }

    /**
     * Checks the form and signs in.
     */
    private void attemptLogin() {
        // Clear old errors
        binding.inputLayoutEmail.setError(null);
        binding.inputLayoutPassword.setError(null);

        // Read the fields
        String email = binding.editEmail.getText() != null
                ? binding.editEmail.getText().toString().trim() : "";
        String password = binding.editPassword.getText() != null
                ? binding.editPassword.getText().toString().trim() : "";

        // Check the fields
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

        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPassword.setError(getString(R.string.msg_enter_password));
            binding.editPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            binding.inputLayoutPassword.setError(getString(R.string.msg_password_min_length));
            binding.editPassword.requestFocus();
            return;
        }

        // Show loading
        setLoadingState(true);

        // Sign in
        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call,
                                   Response<LoginResponse> response) {
                setLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();
                    if (!TextUtils.isEmpty(token)) {
                        // Save the token
                        TokenManager.saveToken(LoginActivity.this, token);

                        Toast.makeText(LoginActivity.this,
                                R.string.msg_login_success,
                                Toast.LENGTH_SHORT).show();

                        navigateToDashboard();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                R.string.msg_login_no_token,
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Sign-in failed
                    String errorMsg = getString(R.string.msg_login_failed);
                    if (response.code() == 401) {
                        errorMsg = getString(R.string.msg_invalid_credentials);
                    } else if (response.code() == 404) {
                        errorMsg = getString(R.string.msg_account_not_found);
                    } else if (response.code() >= 500) {
                        errorMsg = getString(R.string.msg_server_error);
                    }
                    Toast.makeText(LoginActivity.this,
                            errorMsg,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoadingState(false);
                ConnectionToasts.showIfOnline(LoginActivity.this,
                        getString(R.string.msg_network_error, t.getLocalizedMessage()),
                        Toast.LENGTH_LONG);
            }
        });
    }

    /**
     * Disables the form while signing in.
     *
     * @param isLoading {@code true} while waiting
     */
    private void setLoadingState(boolean isLoading) {
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnLogin.setText(isLoading ? R.string.btn_signing_in : R.string.btn_login);
        binding.editEmail.setEnabled(!isLoading);
        binding.editPassword.setEnabled(!isLoading);
        binding.btnGoRegister.setEnabled(!isLoading);
        binding.btnForgotPassword.setEnabled(!isLoading);
    }

    /**
     * Opens home and clears the back stack so Back won't return here.
     */
    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
