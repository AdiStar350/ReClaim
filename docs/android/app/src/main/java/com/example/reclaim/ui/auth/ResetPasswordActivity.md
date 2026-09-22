# ResetPasswordActivity.java

## Overview
This is the final step of the forgot-password flow. An **Activity** is one full screen with its own logic and lifecycle (see `docs/GLOSSARY.md`). It receives the email and a short-lived reset token from `VerifyOtpActivity`, lets the user pick a new password, sends it to the backend, and then signs in automatically with the new password so the user lands on the dashboard already authenticated.

## Concepts used here
- **Activity lifecycle** — `onCreate` reads the extras and wires the button. See `docs/GLOSSARY.md`.
- **Intent extras** — the email and reset token are passed in from the previous screen. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityResetPasswordBinding` exposes the views from `activity_reset_password.xml`. See `docs/GLOSSARY.md`.
- **Retrofit `Call` / `enqueue`** — the reset and login calls run in the background. See `docs/GLOSSARY.md`.
- **Reset token vs login token** — the reset token only authorizes changing the password; the login token (from the follow-up sign-in) authorizes normal use. See `docs/GLOSSARY.md` (JWT).

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity` base class; `WindowCompat` for edge-to-edge.
- **Android framework**: `Intent` reads extras and opens the next screen; `TextUtils` checks blanks; `View` toggles the spinner; `Toast` shows short messages.
- **Project classes**: `ActivityResetPasswordBinding` (layout); `RetrofitClient`/`ReClaimApiService` (API); `ResetPasswordRequest` (JSON body with email, token, new password); `LoginRequest`/`LoginResponse` (the follow-up sign-in); `TokenManager` (saves the login token); `DashboardActivity`/`LoginActivity` (next screens); `ConnectionToasts` (network-aware toast).
- **Retrofit**: `Call`, `Callback`, `Response`.

## Fields
- `public static final String EXTRA_EMAIL / EXTRA_RESET_TOKEN` — intent-extra keys the previous screen uses to pass the email and reset token.
- `private ActivityResetPasswordBinding binding;` — typed access to the views.
- `private ReClaimApiService apiService;` — makes the reset and login calls.
- `private String email;` — the account email; used for both the reset and the follow-up login.
- `private String resetToken;` — proves the code was verified, so the backend allows the password change.

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
- **Where it's called:** From `loginWithNewPassword`'s success path.
- **Why it's written this way:** Once signed in, the reset/verify screens should not remain behind Back, so the flags clear them.
- **Step by step:**
  1. Build an `Intent` to `DashboardActivity`.
  2. Set `NEW_TASK | CLEAR_TASK`.
  3. Start it and finish this screen.

### `private void navigateToLogin()`
```java
private void navigateToLogin() {
    Intent intent = new Intent(this, LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```
- **What it does:** Sends the user to the login screen with a cleared back stack.
- **Where it's called:** From `loginWithNewPassword` when the automatic sign-in fails (both the error response and the network failure).
- **Why it's written this way:** The password was changed successfully, but if the auto-login could not complete, the safe fallback is to ask the user to sign in manually.
- **Step by step:**
  1. Build an `Intent` to `LoginActivity`.
  2. Set `NEW_TASK | CLEAR_TASK`.
  3. Start it and finish this screen.

### `private void setLoading(boolean loading)`
```java
private void setLoading(boolean loading) {
    binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    binding.btnUpdatePassword.setEnabled(!loading);
    binding.editNewPassword.setEnabled(!loading);
    binding.editConfirmNewPassword.setEnabled(!loading);
    binding.btnUpdatePassword.setText(loading
            ? getString(R.string.btn_updating) : getString(R.string.btn_update_password));
}
```
- **What it does:** Shows the spinner and locks the fields and button while a request runs.
- **Where it's called:** From `attemptReset` and its callbacks, and from `loginWithNewPassword`'s callbacks.
- **Why it's written this way:** Prevents a double submit and shows progress with an "Updating…" label.
- **Step by step:**
  1. Show/hide the progress bar.
  2. Enable/disable the update button and both password fields.
  3. Swap the button text between "Update password" and "Updating…".

### `private void loginWithNewPassword(String password)`
```java
private void loginWithNewPassword(@NonNull String password) {
    apiService.login(new LoginRequest(email, password))
            .enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(@NonNull Call<LoginResponse> call,
                                       @NonNull Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null
                            && !TextUtils.isEmpty(response.body().getToken())) {
                        TokenManager.saveToken(ResetPasswordActivity.this,
                                response.body().getToken());
                        Toast.makeText(ResetPasswordActivity.this,
                                R.string.msg_password_updated, Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                        return;
                    }
                    setLoading(false);
                    Toast.makeText(ResetPasswordActivity.this,
                            R.string.msg_reset_sign_in_failed, Toast.LENGTH_LONG).show();
                    navigateToLogin();
                }

                @Override
                public void onFailure(@NonNull Call<LoginResponse> call,
                                      @NonNull Throwable t) {
                    setLoading(false);
                    ConnectionToasts.showIfOnline(ResetPasswordActivity.this,
                            getString(R.string.msg_network_error, t.getLocalizedMessage()),
                            Toast.LENGTH_LONG);
                    navigateToLogin();
                }
            });
}
```
- **What it does:** Signs in with the brand-new password to obtain a real login token, then goes to the dashboard.
- **Where it's called:** From `attemptReset` after the password change succeeds.
- **Why it's written this way:** The reset endpoint returns nothing to log in with, so the app immediately logs in to get a proper token, giving a smooth "changed and signed in" experience. If that follow-up fails, it falls back to the login screen so the user is not stuck.
- **Step by step:**
  1. `enqueue` `apiService.login(...)` with the email and new password.
  2. In `onResponse`: if successful with a non-empty token, save it, toast "password updated", and go to the dashboard.
  3. If the login response is not usable, stop loading, toast "couldn't sign in", and go to the login screen.
  4. In `onFailure`: stop loading, toast a network error if online, and go to the login screen.

### `private void attemptReset()`
```java
private void attemptReset() {
    binding.inputLayoutNewPassword.setError(null);
    binding.inputLayoutConfirmNewPassword.setError(null);

    String password = binding.editNewPassword.getText() != null
            ? binding.editNewPassword.getText().toString().trim() : "";
    String confirm = binding.editConfirmNewPassword.getText() != null
            ? binding.editConfirmNewPassword.getText().toString().trim() : "";

    if (TextUtils.isEmpty(password)) {
        binding.inputLayoutNewPassword.setError(getString(R.string.msg_enter_password_generic));
        binding.editNewPassword.requestFocus();
        return;
    }
    if (password.length() < 6) {
        binding.inputLayoutNewPassword.setError(getString(R.string.msg_password_min_length));
        binding.editNewPassword.requestFocus();
        return;
    }
    if (!password.equals(confirm)) {
        binding.inputLayoutConfirmNewPassword.setError(getString(R.string.msg_passwords_mismatch));
        binding.editConfirmNewPassword.requestFocus();
        return;
    }

    setLoading(true);
    apiService.resetPassword(new ResetPasswordRequest(email, resetToken, password))
            .enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call,
                                       @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        loginWithNewPassword(password);
                        return;
                    }
                    setLoading(false);
                    binding.inputLayoutNewPassword.setError(
                            getString(R.string.msg_reset_invalid));
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    setLoading(false);
                    ConnectionToasts.showIfOnline(ResetPasswordActivity.this,
                            getString(R.string.msg_network_error, t.getLocalizedMessage()),
                            Toast.LENGTH_LONG);
                }
            });
}
```
- **What it does:** Validates the new password, sends it with the reset token, and on success chains into the automatic sign-in.
- **Where it's called:** From the "update password" button, wired in `onCreate`.
- **Why it's written this way:** Local checks (not blank, at least 6 characters, both fields match) run before the network. On a failed reset it keeps the user here and shows "the reset link is invalid or expired" rather than moving on. Note it does not call `setLoading(false)` before chaining to `loginWithNewPassword`, so the spinner stays up across both requests.
- **Step by step:**
  1. Clear old errors and read both password fields (trimmed).
  2. Run the checks (present, length ≥ 6, match); each failure shows an error and refocuses.
  3. Show loading and `enqueue` `resetPassword` with a `ResetPasswordRequest(email, resetToken, password)`.
  4. In `onResponse`: on success, call `loginWithNewPassword(password)`; otherwise stop loading and show "reset invalid".
  5. In `onFailure`: stop loading and toast a network error if online.

### `protected void onCreate(Bundle savedInstanceState)`
```java
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
```
- **What it does:** Builds the screen, reads the email and reset token, and wires the toolbar back and update buttons.
- **Where it's called:** Android calls it when the Activity is created.
- **Why it's written this way:** Without both the email and the reset token the screen cannot do its job, so it closes immediately with a generic error rather than showing a form that would always fail.
- **Step by step:**
  1. Call `super.onCreate`, go edge-to-edge, inflate `activity_reset_password.xml`, set content.
  2. Get the API service and read the `EXTRA_EMAIL` and `EXTRA_RESET_TOKEN` extras.
  3. If either is blank, toast an error, finish, and return.
  4. Wire the toolbar back navigation and the update-password button (`attemptReset`).
