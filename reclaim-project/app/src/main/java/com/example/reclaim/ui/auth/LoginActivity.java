package com.example.reclaim.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.reclaim.databinding.ActivityLoginBinding;
import com.example.reclaim.network.LoginRequest;
import com.example.reclaim.network.LoginResponse;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.ui.dashboard.DashboardActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login screen for the ReClaim application.
 * <p>
 * Provides email and password input fields, a login button, a link to
 * navigate to {@link RegisterActivity}, and a forgot-password action.
 * On successful authentication, the JWT token is persisted via
 * {@link TokenManager} and the user is navigated to
 * {@link DashboardActivity}.
 * </p>
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ReClaimApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the API service singleton
        apiService = RetrofitClient.getApiService();

        // If user is already logged in, skip to dashboard
        if (TokenManager.hasToken(this)) {
            navigateToDashboard();
            return;
        }

        setupClickListeners();
    }

    /**
     * Configures click listeners for all interactive elements on the login screen.
     */
    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.btnGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        binding.btnForgotPassword.setOnClickListener(v -> {
            // TODO: Implement forgot-password flow
            Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Extracts email and password from the input fields, validates them,
     * and initiates the login API call.
     */
    private void attemptLogin() {
        // Clear previous errors
        binding.inputLayoutEmail.setError(null);
        binding.inputLayoutPassword.setError(null);

        // Extract text from inputs
        String email = binding.editEmail.getText() != null
                ? binding.editEmail.getText().toString().trim() : "";
        String password = binding.editPassword.getText() != null
                ? binding.editPassword.getText().toString().trim() : "";

        // ── Validate inputs ──────────────────────────────────────────
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

        if (TextUtils.isEmpty(password)) {
            binding.inputLayoutPassword.setError("Please enter your password");
            binding.editPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            binding.inputLayoutPassword.setError("Password must be at least 6 characters");
            binding.editPassword.requestFocus();
            return;
        }

        // ── Show loading state ───────────────────────────────────────
        setLoadingState(true);

        // ── Make the API call ────────────────────────────────────────
        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call,
                                   Response<LoginResponse> response) {
                setLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();
                    if (!TextUtils.isEmpty(token)) {
                        // Save the JWT token to SharedPreferences
                        TokenManager.saveToken(LoginActivity.this, token);

                        Toast.makeText(LoginActivity.this,
                                "Login successful!",
                                Toast.LENGTH_SHORT).show();

                        navigateToDashboard();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Login failed: no token received",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Server returned an error (401, 403, 500, etc.)
                    String errorMsg = "Login failed";
                    if (response.code() == 401) {
                        errorMsg = "Invalid email or password";
                    } else if (response.code() == 404) {
                        errorMsg = "Account not found";
                    } else if (response.code() >= 500) {
                        errorMsg = "Server error. Please try again later.";
                    }
                    Toast.makeText(LoginActivity.this,
                            errorMsg,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoadingState(false);

                // Network error (no internet, timeout, DNS failure, etc.)
                Toast.makeText(LoginActivity.this,
                        "Network error: " + t.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Toggles the UI between a loading and an interactive state.
     * <p>
     * While loading, the login button is disabled and its text changes
     * to a progress indicator. Input fields are also disabled to prevent
     * double-submission.
     * </p>
     *
     * @param isLoading {@code true} to show loading state, {@code false} to restore
     */
    private void setLoadingState(boolean isLoading) {
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnLogin.setText(isLoading ? "Signing in…" : "Sign In");
        binding.editEmail.setEnabled(!isLoading);
        binding.editPassword.setEnabled(!isLoading);
        binding.btnGoRegister.setEnabled(!isLoading);
        binding.btnForgotPassword.setEnabled(!isLoading);
    }

    /**
     * Navigates to {@link DashboardActivity} and clears the back stack
     * so the user cannot press Back to return to the login screen.
     */
    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
