```java
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
```

What that function does: I show the code form for sign-up or password reset.
What the function is responsible for: I am the verify-otp screen start.
Where it works: RegisterActivity starts me in register mode. LoginActivity and ProfileSettingsFragment start me in reset mode. I inflate activity_verify_otp.xml.
Why I chose to write the code of the function the way its written: Missing email closes me. The subtitle and button text change with mode.
More: I use ActivityVerifyOtpBinding, EXTRA_EMAIL, EXTRA_MODE, and WindowCompat.

```java
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
```

What that function does: I check the code is 6 digits, then I pick register or reset verify.
What the function is responsible for: I am the verify button path.
Where it works: I am called from btnVerify.
Why I chose to write the code of the function the way its written: One form, two API routes, split after the length check.
More: I use MODE_RESET to choose the call.

```java
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
```

What that function does: I check the sign-up code, save the token, and open home.
What the function is responsible for: I am POST register/verify-otp from this screen.
Where it works: I am called from attemptVerify in register mode.
Why I chose to write the code of the function the way its written: A token means the account was created, so I sign in the same way as login.
More: I use VerifyOtpRequest, TokenManager.saveToken, and LoginResponse.

```java
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
```

What that function does: I save the sign-up token or show a code error.
What the function is responsible for: I am the Retrofit success path for registration verify.
Where it works: Retrofit calls me from verifyRegistrationCode.
Why I chose to write the code of the function the way its written: handleVerifyError maps 410/429/400 to field text.
More: I use TokenManager.saveToken.

```java
public void onFailure(@NonNull Call<LoginResponse> call,
                      @NonNull Throwable t) {
    setLoading(false);
    ConnectionToasts.showIfOnline(VerifyOtpActivity.this,
            getString(R.string.msg_network_error, t.getLocalizedMessage()),
            Toast.LENGTH_LONG);
}
```

What that function does: I stop loading and toast a network error if the API is up.
What the function is responsible for: I am the Retrofit failure path for registration verify.
Where it works: Retrofit calls me from verifyRegistrationCode.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline.

```java
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
```

What that function does: I check the reset code and open the new-password screen with a reset token.
What the function is responsible for: I am POST forgot-password/verify-otp from this screen.
Where it works: I am called from attemptVerify in reset mode. Success starts ResetPasswordActivity.
Why I chose to write the code of the function the way its written: I do not save a login token here. The reset token is only for the next screen.
More: I use ResetTokenResponse.getResetToken and VerifyOtpRequest.

```java
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
```

What that function does: I pass email and reset token to ResetPasswordActivity, or I show a code error.
What the function is responsible for: I am the Retrofit success path for reset verify.
Where it works: Retrofit calls me from verifyResetCode.
Why I chose to write the code of the function the way its written: An empty reset token is treated like a bad code.
More: I use ResetPasswordActivity extras.

```java
public void onFailure(@NonNull Call<ResetTokenResponse> call,
                      @NonNull Throwable t) {
    setLoading(false);
    ConnectionToasts.showIfOnline(VerifyOtpActivity.this,
            getString(R.string.msg_network_error, t.getLocalizedMessage()),
            Toast.LENGTH_LONG);
}
```

What that function does: I stop loading and toast a network error if the API is up.
What the function is responsible for: I am the Retrofit failure path for reset verify.
Where it works: Retrofit calls me from verifyResetCode.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline.

```java
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
```

What that function does: I show expired, too-many, or invalid on the code field.
What the function is responsible for: I map HTTP codes to OTP errors.
Where it works: I am called from both verify callbacks.
Why I chose to write the code of the function the way its written: 410 and 429 are special. Everything else looks like a wrong code.
More: I set inputLayoutOtp error text.

```java
private void resendCode() {
    setLoading(true);
    Callback<Void> callback = new Callback<Void>() {
        @Override
        public void onResponse(@NonNull Call<Void> call,
                               @NonNull Response<Void> response) {
            setLoading(false);
            int msg = response.isSuccessful()
                    ? R.string.msg_otp_sent : R.string.error_generic;
            if (response.isSuccessful()) {
                Toast.makeText(VerifyOtpActivity.this, msg, Toast.LENGTH_SHORT).show();
            } else {
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
```

What that function does: I send a new code for reset or sign-up.
What the function is responsible for: I am the resend button path.
Where it works: I am called from btnResendCode.
Why I chose to write the code of the function the way its written: One callback, two requests, chosen by mode.
More: I use ForgotPasswordRequest or ResendOtpRequest.

```java
public void onResponse(@NonNull Call<Void> call,
                       @NonNull Response<Void> response) {
    setLoading(false);
    int msg = response.isSuccessful()
            ? R.string.msg_otp_sent : R.string.error_generic;
    if (response.isSuccessful()) {
        Toast.makeText(VerifyOtpActivity.this, msg, Toast.LENGTH_SHORT).show();
    } else {
        ConnectionToasts.showIfOnline(VerifyOtpActivity.this, msg, Toast.LENGTH_SHORT);
    }
}
```

What that function does: I toast that a new code was sent, or I show a generic error if online.
What the function is responsible for: I am the Retrofit success path for resend.
Where it works: Retrofit calls me from resendCode.
Why I chose to write the code of the function the way its written: Success is a real toast. Failure uses ConnectionToasts.
More: I use Toast and ConnectionToasts.

```java
public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
    setLoading(false);
    ConnectionToasts.showIfOnline(VerifyOtpActivity.this,
            getString(R.string.msg_network_error, t.getLocalizedMessage()),
            Toast.LENGTH_LONG);
}
```

What that function does: I stop loading and toast a network error if the API is up.
What the function is responsible for: I am the Retrofit failure path for resend.
Where it works: Retrofit calls me from resendCode.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline.

```java
private void setLoading(boolean loading) {
    binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    binding.btnVerify.setEnabled(!loading);
    binding.btnResendCode.setEnabled(!loading);
    binding.editOtp.setEnabled(!loading);
    int idleText = MODE_RESET.equals(mode) ? R.string.btn_continue : R.string.btn_verify;
    binding.btnVerify.setText(loading ? getString(R.string.btn_please_wait) : getString(idleText));
}
```

What that function does: I show a spinner and lock the form while a call runs.
What the function is responsible for: I stop a second verify or resend.
Where it works: I am called from verify and resend methods.
Why I chose to write the code of the function the way its written: The idle button text depends on mode.
More: I toggle progress visibility and enabled flags.

```java
private void navigateToDashboard() {
    Intent intent = new Intent(this, DashboardActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

What that function does: I open home and clear the back stack.
What the function is responsible for: I am post-signup navigation.
Where it works: I am called after a successful registration verify.
Why I chose to write the code of the function the way its written: CLEAR_TASK drops register and verify from Back.
More: I use Intent flags NEW_TASK and CLEAR_TASK.
