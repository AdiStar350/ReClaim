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
import com.example.reclaim.databinding.ActivityVerifyOtpBinding;
import com.example.reclaim.network.ForgotPasswordRequest;
import com.example.reclaim.network.LoginResponse;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.ResendOtpRequest;
import com.example.reclaim.network.ResetTokenResponse;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.network.VerifyOtpRequest;
import com.example.reclaim.ui.dashboard.DashboardActivity;
import com.example.reclaim.util.ConnectionToasts;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Sign-up step 2: enter the email code. On success, the account is created and home opens.
 */
public class VerifyOtpActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String MODE_REGISTER = "register";
    public static final String MODE_RESET = "reset";

    private ActivityVerifyOtpBinding binding;
    private ReClaimApiService apiService;
    private String email;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityVerifyOtpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getApiService();
        email = getIntent().getStringExtra(EXTRA_EMAIL);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) {
            mode = MODE_REGISTER;
        }

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boolean isReset = MODE_RESET.equals(mode);
        int subtitleRes = isReset ? R.string.verify_subtitle_reset : R.string.verify_subtitle;
        binding.textVerifySubtitle.setText(getString(subtitleRes, email));
        binding.btnVerify.setText(isReset ? R.string.btn_continue : R.string.btn_verify);

        binding.toolbarVerify.setNavigationOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnVerify.setOnClickListener(v -> attemptVerify());
        binding.btnResendCode.setOnClickListener(v -> resendCode());
    }

    private void attemptVerify() {
        binding.inputLayoutOtp.setError(null);

        String code = binding.editOtp.getText() != null
                ? binding.editOtp.getText().toString().trim() : "";

        if (code.length() != 6) {
            binding.inputLayoutOtp.setError(getString(R.string.msg_otp_required));
            binding.editOtp.requestFocus();
            return;
        }

        if (MODE_RESET.equals(mode)) {
            verifyResetCode(code);
        } else {
            verifyRegistrationCode(code);
        }
    }

    private void verifyRegistrationCode(String code) {
        setLoading(true);
        apiService.verifyRegistrationOtp(new VerifyOtpRequest(email, code))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginResponse> call,
                                           @NonNull Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null
                                && !TextUtils.isEmpty(response.body().getToken())) {
                            TokenManager.saveToken(VerifyOtpActivity.this,
                                    response.body().getToken());
                            Toast.makeText(VerifyOtpActivity.this,
                                    R.string.msg_registration_complete,
                                    Toast.LENGTH_LONG).show();
                            navigateToDashboard();
                        } else {
                            handleVerifyError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginResponse> call,
                                          @NonNull Throwable t) {
                        setLoading(false);
                        ConnectionToasts.showIfOnline(VerifyOtpActivity.this,
                                getString(R.string.msg_network_error, t.getLocalizedMessage()),
                                Toast.LENGTH_LONG);
                    }
                });
    }

    private void verifyResetCode(String code) {
        setLoading(true);
        apiService.verifyPasswordResetOtp(new VerifyOtpRequest(email, code))
                .enqueue(new Callback<ResetTokenResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ResetTokenResponse> call,
                                           @NonNull Response<ResetTokenResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null
                                && !TextUtils.isEmpty(response.body().getResetToken())) {
                            Intent intent = new Intent(VerifyOtpActivity.this,
                                    ResetPasswordActivity.class);
                            intent.putExtra(ResetPasswordActivity.EXTRA_EMAIL, email);
                            intent.putExtra(ResetPasswordActivity.EXTRA_RESET_TOKEN,
                                    response.body().getResetToken());
                            startActivity(intent);
                        } else {
                            handleVerifyError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResetTokenResponse> call,
                                          @NonNull Throwable t) {
                        setLoading(false);
                        ConnectionToasts.showIfOnline(VerifyOtpActivity.this,
                                getString(R.string.msg_network_error, t.getLocalizedMessage()),
                                Toast.LENGTH_LONG);
                    }
                });
    }

    private void handleVerifyError(int statusCode) {
        int messageRes;
        switch (statusCode) {
            case 410:
                messageRes = R.string.msg_otp_expired;
                break;
            case 429:
                messageRes = R.string.msg_otp_too_many;
                break;
            case 400:
            default:
                messageRes = R.string.msg_otp_invalid;
                break;
        }
        binding.inputLayoutOtp.setError(getString(messageRes));
    }

    private void resendCode() {
        setLoading(true);
        Callback<Void> callback = new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(VerifyOtpActivity.this,
                            R.string.msg_otp_sent, Toast.LENGTH_SHORT).show();
                } else {
                    // 500 means the code email could not be sent.
                    int msg = response.code() == 500
                            ? R.string.msg_otp_send_failed : R.string.error_generic;
                    ConnectionToasts.showIfOnline(VerifyOtpActivity.this, msg, Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setLoading(false);
                ConnectionToasts.showIfOnline(VerifyOtpActivity.this,
                        getString(R.string.msg_network_error, t.getLocalizedMessage()),
                        Toast.LENGTH_LONG);
            }
        };

        if (MODE_RESET.equals(mode)) {
            apiService.sendPasswordResetOtp(new ForgotPasswordRequest(email)).enqueue(callback);
        } else {
            apiService.resendRegistrationOtp(new ResendOtpRequest(email)).enqueue(callback);
        }
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnVerify.setEnabled(!loading);
        binding.btnResendCode.setEnabled(!loading);
        binding.editOtp.setEnabled(!loading);
        int idleText = MODE_RESET.equals(mode) ? R.string.btn_continue : R.string.btn_verify;
        binding.btnVerify.setText(loading ? getString(R.string.btn_please_wait) : getString(idleText));
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
