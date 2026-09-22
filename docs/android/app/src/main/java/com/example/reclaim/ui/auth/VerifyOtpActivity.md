# VerifyOtpActivity.java

## Overview
This screen is the "enter the code" step shared by two flows: finishing sign-up and resetting a password. An **Activity** is one full screen with its own logic and lifecycle (see `docs/GLOSSARY.md`). It reads the email and a mode from the `Intent` that opened it, lets the user type the 6-digit code (OTP), and calls the matching backend endpoint. In register mode a correct code creates the account and opens the dashboard; in reset mode it hands a reset token to `ResetPasswordActivity`.

## Concepts used here
- **Activity lifecycle** — `onCreate` reads the intent extras and builds the screen. See `docs/GLOSSARY.md`.
- **Intent extras** — the email and mode are passed in from the previous screen. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityVerifyOtpBinding` exposes the views from `activity_verify_otp.xml`. See `docs/GLOSSARY.md`.
- **Retrofit `Call` / `enqueue`** — verify and resend calls run in the background. See `docs/GLOSSARY.md`.
- **OTP** — the emailed one-time code. See `docs/GLOSSARY.md`.
- **HTTP status codes** — `410` expired, `429` too many tries, `400` invalid. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity` base class; `WindowCompat` for edge-to-edge.
- **Android framework**: `Intent` reads extras and opens the next screen; `TextUtils` checks blanks; `View` toggles the progress bar; `Toast` shows short messages.
- **Project classes**: `ActivityVerifyOtpBinding` (layout); `RetrofitClient`/`ReClaimApiService` (API); `VerifyOtpRequest`, `ResendOtpRequest`, `ForgotPasswordRequest` (JSON bodies); `LoginResponse` (holds the login token for register mode); `ResetTokenResponse` (holds the reset token for reset mode); `TokenManager` (saves the login token); `DashboardActivity`/`ResetPasswordActivity` (next screens); `ConnectionToasts` (network-aware toast).
- **Retrofit**: `Call`, `Callback`, `Response`.

## Fields
- `public static final String EXTRA_EMAIL / EXTRA_MODE` — intent-extra keys other screens use to pass in the email and which flow this is.
- `public static final String MODE_REGISTER / MODE_RESET` — the two allowed mode values.
- `private ActivityVerifyOtpBinding binding;` — typed access to the views.
- `private ReClaimApiService apiService;` — makes the verify/resend calls.
- `private String email;` — the address the code was sent to; passed to every call.
- `private String mode;` — either register or reset; decides which endpoints and texts are used.

## Functions

### `private void navigateToDashboard()`
```java
private void navigateToDashboard() {
    Intent intent = new Intent(this, DashboardActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```
- **What it does:** Opens the home screen and clears the back stack.
- **Where it's called:** From `verifyRegistrationCode`'s success path (inside its `Callback`).
- **Why it's written this way:** After the account is created the user is signed in, so the sign-up screens should not stay behind the Back button; the two flags wipe them.
- **Step by step:**
  1. Build an `Intent` to `DashboardActivity`.
  2. Set `NEW_TASK | CLEAR_TASK` to start fresh and clear history.
  3. Start it and `finish()` this screen.

### `private void setLoading(boolean loading)`
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
- **What it does:** Shows or hides the spinner and locks the buttons and code field while a request runs.
- **Where it's called:** From `attemptVerify`'s helpers (`verifyRegistrationCode`, `verifyResetCode`) and `resendCode`, and from their callbacks.
- **Why it's written this way:** Blocking input during the call prevents double taps. The idle button label depends on the mode ("Continue" for reset, "Verify" for register).
- **Step by step:**
  1. Show/hide the progress bar.
  2. Enable/disable the verify button, resend button, and code field.
  3. Set the button text to "Please wait" while loading, or the mode-specific idle label otherwise.

### `private void handleVerifyError(int statusCode)`
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
- **What it does:** Shows the right error under the code field when verification fails.
- **Where it's called:** From `verifyRegistrationCode` and `verifyResetCode` when the response is not successful.
- **Why it's written this way:** Mapping status codes to messages tells the user whether the code expired (`410`), they tried too often (`429`), or it was simply wrong (`400`/other).
- **Step by step:**
  1. Choose a message from the status code.
  2. Set it as the error on the OTP input layout.

### `private void verifyRegistrationCode(String code)`
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
- **What it does:** Confirms the sign-up code; on success the backend creates the account and returns a login token, so the user is signed straight in.
- **Where it's called:** From `attemptVerify` when the mode is register.
- **Why it's written this way:** A missing or empty token is treated as a failure, so the app never thinks it is signed in without a real token. The token is saved immediately so later screens are authenticated.
- **Step by step:**
  1. Show loading.
  2. `enqueue` `verifyRegistrationOtp` with a `VerifyOtpRequest(email, code)`.
  3. In `onResponse`: stop loading; if successful with a non-empty token, save it, toast "registration complete", and go to the dashboard.
  4. Otherwise call `handleVerifyError(code)`.
  5. In `onFailure`: stop loading and toast a network error if online.

### `private void verifyResetCode(String code)`
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
- **What it does:** Confirms the password-reset code; on success it receives a short-lived reset token and passes it to the screen where the new password is chosen.
- **Where it's called:** From `attemptVerify` when the mode is reset.
- **Why it's written this way:** Reset returns a token (not a login), so instead of signing in, this hands the token to `ResetPasswordActivity`, which uses it to authorize the password change.
- **Step by step:**
  1. Show loading.
  2. `enqueue` `verifyPasswordResetOtp` with `VerifyOtpRequest(email, code)`.
  3. In `onResponse`: stop loading; if successful with a non-empty reset token, start `ResetPasswordActivity` with the email and reset-token extras.
  4. Otherwise call `handleVerifyError(code)`.
  5. In `onFailure`: stop loading and toast a network error if online.

### `private void attemptVerify()`
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
- **What it does:** Validates that the code is 6 digits, then routes to the register or reset verification depending on the mode.
- **Where it's called:** From the Verify/Continue button, wired in `onCreate`.
- **Why it's written this way:** A single entry point checks the length once and then branches on `mode`, so both flows share the same button and field.
- **Step by step:**
  1. Clear the old error and read the trimmed code.
  2. If it is not exactly 6 characters, show an error and refocus.
  3. Otherwise call `verifyResetCode` or `verifyRegistrationCode` based on the mode.

### `private void resendCode()`
```java
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
```
- **What it does:** Asks the backend to email a fresh code, using the right endpoint for the current mode.
- **Where it's called:** From the "resend code" button, wired in `onCreate`.
- **Why it's written this way:** One shared `Callback` handles both endpoints so the success/error handling is written once; only the endpoint chosen at the end differs by mode.
- **Step by step:**
  1. Show loading and build a shared `Callback<Void>`.
  2. In the callback's `onResponse`: stop loading; on success toast "code sent"; on `500` toast "send failed"; otherwise a generic toast (only if online).
  3. In `onFailure`: stop loading and toast a network error if online.
  4. If in reset mode, call `sendPasswordResetOtp`; otherwise `resendRegistrationOtp` — both with the shared callback.

### `protected void onCreate(Bundle savedInstanceState)`
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
- **What it does:** Reads the email and mode, adjusts the on-screen text and button label to match the flow, and wires the toolbar back, verify, and resend buttons.
- **Where it's called:** Android calls it when the Activity is created.
- **Why it's written this way:** The mode defaults to register if not provided, and a missing email is a bug in the caller, so the screen closes right away with a generic error rather than showing a broken form.
- **Step by step:**
  1. Call `super.onCreate`, go edge-to-edge, inflate `activity_verify_otp.xml`, and set content.
  2. Get the API service and read the `EXTRA_EMAIL` and `EXTRA_MODE` extras; default the mode to register.
  3. If the email is blank, toast an error, finish, and return.
  4. Set the subtitle and button label based on whether this is a reset.
  5. Wire the toolbar navigation (Back), the verify button (`attemptVerify`), and the resend button (`resendCode`).
