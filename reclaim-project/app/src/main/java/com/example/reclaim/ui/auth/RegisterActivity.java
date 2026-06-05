package com.example.reclaim.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.reclaim.databinding.ActivityRegisterBinding;
import com.example.reclaim.model.User;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RegisterRequest;
import com.example.reclaim.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Registration screen for the ReClaim application.
 * <p>
 * Allows new users to create an account by providing their full name,
 * email, phone number, and password. Validates all fields locally
 * before making an asynchronous Retrofit call to
 * {@code POST /api/auth/register}. On success, finishes back to
 * {@link LoginActivity}.
 * </p>
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private ReClaimApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the API service singleton
        apiService = RetrofitClient.getApiService();

        setupClickListeners();
    }

    /**
     * Configures click listeners for the registration screen.
     */
    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> attemptRegistration());

        binding.btnGoLogin.setOnClickListener(v -> {
            // Return to the existing LoginActivity on the back stack
            finish();
        });
    }

    /**
     * Extracts and validates all input fields, then initiates the
     * registration API call.
     */
    private void attemptRegistration() {
        // ── Clear previous errors ────────────────────────────────────
        binding.inputLayoutFullName.setError(null);
        binding.inputLayoutEmail.setError(null);
        binding.inputLayoutPhone.setError(null);
        binding.inputLayoutPassword.setError(null);
        binding.inputLayoutConfirmPassword.setError(null);

        // ── Extract text from inputs ─────────────────────────────────
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

        // ── Validate inputs ──────────────────────────────────────────
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

        // ── Show loading state ───────────────────────────────────────
        setLoadingState(true);

        // ── Make the API call ────────────────────────────────────────
        RegisterRequest request = new RegisterRequest(email, password, fullName, phone);

        apiService.register(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                setLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    // Registration successful — navigate back to Login
                    Toast.makeText(RegisterActivity.this,
                            "Registration successful! Please sign in.",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    // Server returned an error
                    handleRegistrationError(response.code());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                setLoadingState(false);

                // Network error (no internet, timeout, DNS failure, etc.)
                Toast.makeText(RegisterActivity.this,
                        "Network error: " + t.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Displays a contextual error Toast based on the HTTP status code.
     *
     * @param statusCode the HTTP status code from the server response
     */
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
     * Toggles the UI between a loading and an interactive state.
     * <p>
     * While loading, the register button is disabled and its text changes
     * to a progress indicator. All input fields are also disabled to
     * prevent double-submission.
     * </p>
     *
     * @param isLoading {@code true} to show loading state, {@code false} to restore
     */
    private void setLoadingState(boolean isLoading) {
        binding.btnRegister.setEnabled(!isLoading);
        binding.btnRegister.setText(isLoading ? "Creating Account…" : "Create Account");
        binding.editFullName.setEnabled(!isLoading);
        binding.editEmail.setEnabled(!isLoading);
        binding.editPhone.setEnabled(!isLoading);
        binding.editPassword.setEnabled(!isLoading);
        binding.editConfirmPassword.setEnabled(!isLoading);
        binding.btnGoLogin.setEnabled(!isLoading);
    }
}
