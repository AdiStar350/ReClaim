# LoginActivity.java

## Overview
This is the sign-in screen. An **Activity** is one full screen with its own logic and lifecycle (see `docs/GLOSSARY.md`). When the app opens and there is no saved login, this screen shows an email/password form; when the form is submitted it asks the backend to sign the user in, saves the returned token, and moves on to the home screen. It also holds the small "forgot password" flow that emails a reset code.

## Concepts used here
- **Activity lifecycle** — Android calls `onCreate` when the screen is created; setup goes there. See `docs/GLOSSARY.md`.
- **ViewBinding** — `ActivityLoginBinding` gives typed access to the views from `activity_login.xml`, so no `findViewById` is needed. See `docs/GLOSSARY.md`.
- **Intent** — a message object used to open another Activity (register, verify, dashboard). See `docs/GLOSSARY.md`.
- **Retrofit `Call` / `enqueue` / `Callback`** — the network call runs in the background and reports back on `onResponse`/`onFailure`. See `docs/GLOSSARY.md`.
- **HTTP status codes** — the code checks `401`, `404`, `400`, `500` to pick the right message. See `docs/GLOSSARY.md`.
- **SharedPreferences (via `TokenManager`)** — the login token is saved on the phone. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX / AppCompat**: `AppCompatActivity` is the base class every screen extends. `WindowCompat.setDecorFitsSystemWindows` lets the layout draw behind the status/navigation bars. `AlertDialog` and `MaterialAlertDialogBuilder` build the forgot-password popup.
- **Android framework**: `Intent` opens other screens; `TextUtils.isEmpty` checks for blank text; `Patterns.EMAIL_ADDRESS` is a ready-made email checker; `Toast` shows a short popup message; `LayoutInflater` turns the dialog XML into real views; `Button`/`View` are basic widgets.
- **Material**: `TextInputLayout` and `TextInputEditText` are the styled text fields used in the reset-email dialog.
- **Project classes**: `ActivityLoginBinding` (generated from the layout), `RetrofitClient` (builds the API client), `ReClaimApiService` (the list of backend calls), `TokenManager` (saves/reads the login token), `LoginRequest`/`LoginResponse`/`ForgotPasswordRequest` (the JSON shapes for the calls), `DashboardActivity`/`RegisterActivity`/`VerifyOtpActivity` (screens this one opens), and `ConnectionToasts` (shows a network error only when the backend is actually reachable).
- **Retrofit**: `Call`, `Callback`, `Response` are used to send requests and read replies.

## Fields
- `private ActivityLoginBinding binding;` — the typed handle to all views in `activity_login.xml`. It exists so the code can read the email/password fields and toggle the buttons.
- `private ReClaimApiService apiService;` — the object that makes the backend calls (login, send reset code). It is created once in `onCreate`.

## Functions

### `private void navigateToDashboard()`
```java
private void navigateToDashboard() {
    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```
- **What it does:** Opens the home screen and clears the back history so pressing Back does not return to login.
- **Where it's called:** From `onCreate` (when a token already exists) and from `attemptLogin`'s success path (inside the login `Callback`).
- **Why it's written this way:** The two intent flags (`NEW_TASK` + `CLEAR_TASK`) wipe the login/register screens off the back stack, so a signed-in user cannot accidentally back into the sign-in form. `finish()` closes this screen too.
- **Step by step:**
  1. Build an `Intent` aimed at `DashboardActivity`.
  2. Set flags so the dashboard starts a fresh task and clears everything behind it.
  3. Start the dashboard.
  4. Finish this activity so it is removed.

### `private void setLoadingState(boolean isLoading)`
```java
private void setLoadingState(boolean isLoading) {
    binding.btnLogin.setEnabled(!isLoading);
    binding.btnLogin.setText(isLoading ? R.string.btn_signing_in : R.string.btn_login);
    binding.editEmail.setEnabled(!isLoading);
    binding.editPassword.setEnabled(!isLoading);
    binding.btnGoRegister.setEnabled(!isLoading);
    binding.btnForgotPassword.setEnabled(!isLoading);
}
```
- **What it does:** Turns the whole form on or off while a login request is in flight, and changes the button text to "Signing in…".
- **Where it's called:** From `attemptLogin`, and from both the `onResponse` and `onFailure` callbacks of the login request.
- **Why it's written this way:** Disabling every input during the network call blocks a second submit and makes it clear the app is busy.
- **Step by step:**
  1. Enable or disable the login button based on `isLoading` (disabled while loading).
  2. Swap the button label between "Login" and "Signing in…".
  3. Disable/enable the email field, password field, register button, and forgot-password button together.

### `private void sendPasswordResetOtp(String email, AlertDialog dialog, Button sendButton, TextInputLayout emailLayout, TextInputEditText emailInput)`
```java
private void sendPasswordResetOtp(@NonNull String email,
                                  @NonNull AlertDialog dialog,
                                  @NonNull Button sendButton,
                                  @NonNull TextInputLayout emailLayout,
                                  @NonNull TextInputEditText emailInput) {
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
                        return;
                    }
                    if (response.code() == 404 || response.code() == 400) {
                        emailLayout.setError(getString(R.string.msg_no_account_for_email));
                        emailInput.requestFocus();
                        return;
                    }
                    // 500 means the server could not send the email.
                    int msg = response.code() == 500
                            ? R.string.msg_otp_send_failed : R.string.error_generic;
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
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
```
- **What it does:** Asks the backend to email a password-reset code for the given address, then opens the verify-code screen in "reset" mode on success.
- **Where it's called:** From `showForgotPasswordDialog`, when the dialog's Send button is tapped and the email passes validation.
- **Why it's written this way:** It disables both dialog buttons while waiting so the user cannot tap twice. On a `404`/`400` it keeps the dialog open and shows the error on the field, so an unknown or mistyped email can be fixed without closing the popup. The `isFinishing()` guard avoids touching views after the screen is gone.
- **Step by step:**
  1. Disable the Send and Cancel buttons.
  2. Call `apiService.sendPasswordResetOtp(...)` with a `ForgotPasswordRequest` holding the email, and `enqueue` it (run in background).
  3. In the anonymous `Callback`'s `onResponse`: if the screen is finishing, stop. Otherwise re-enable the buttons.
  4. On success, close the dialog, toast "code sent", and start `VerifyOtpActivity` with the email and `MODE_RESET` extra.
  5. On `404`/`400`, show "no account for this email" on the field and refocus it.
  6. On `500`, show "could not send the code"; any other error shows a generic message.
  7. In `onFailure` (no reply at all): re-enable the buttons and, only if the backend is actually online, toast the network error via `ConnectionToasts`.

### `private void showForgotPasswordDialog()`
```java
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
            sendPasswordResetOtp(email, dialog, send, emailLayout, emailInput);
        });
    });
    dialog.show();
}
```
- **What it does:** Shows a popup asking for the account email, pre-filled with whatever was typed on the login form, and (on a valid email) triggers the reset-code request.
- **Where it's called:** From the "forgot password" button, wired in `setupClickListeners`.
- **Why it's written this way:** The Send button's real click handler is attached inside `setOnShowListener` (not via `setPositiveButton`) so a blank/invalid email leaves the dialog open instead of closing it automatically. Copying the already-typed email saves the user retyping.
- **Step by step:**
  1. Inflate `dialog_forgot_password.xml` and grab its email field and error container.
  2. If the login email field already has text, copy it into the dialog's field.
  3. Build a Material dialog with Cancel and "Send reset code" buttons (both handlers left null for now).
  4. When the dialog is shown, attach a click handler to the positive button that: clears old errors, reads and trims the email, and validates it with `Patterns.EMAIL_ADDRESS`.
  5. If invalid, show the field error and stop; otherwise call `sendPasswordResetOtp(...)`.
  6. Show the dialog.

### `private void attemptLogin()`
```java
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
```
- **What it does:** Validates the email/password, then calls the backend to log in. On success it saves the token and opens the dashboard; on failure it shows a reason.
- **Where it's called:** From the Login button, wired in `setupClickListeners`.
- **Why it's written this way:** All checks happen locally first (blank email, valid email shape, blank password, minimum length) so obvious mistakes never hit the network. Different status codes map to clearer messages, and an empty token is treated as a failure rather than a fake success.
- **Step by step:**
  1. Clear any previous field errors.
  2. Read and trim the email and password.
  3. Run four checks in order; each failing check sets a field error, refocuses, and returns.
  4. Call `setLoadingState(true)` to lock the form.
  5. Build a `LoginRequest` and `enqueue` `apiService.login(...)`.
  6. In `onResponse`: stop loading; if successful with a non-empty token, save it via `TokenManager`, toast success, and go to the dashboard.
  7. If the body/token is missing, toast "no token".
  8. If the response was an error, pick a message from the status code (`401` invalid credentials, `404` account not found, `5xx` server error, else generic) and toast it.
  9. In `onFailure`: stop loading and toast the network error only if the backend is online.

### `private void setupClickListeners()`
```java
private void setupClickListeners() {
    binding.btnLogin.setOnClickListener(v -> attemptLogin());

    binding.btnGoRegister.setOnClickListener(v -> {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    });

    binding.btnForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
}
```
- **What it does:** Connects the three buttons (login, go to register, forgot password) to their actions.
- **Where it's called:** Once from `onCreate`.
- **Why it's written this way:** Small lambdas keep each button's action next to where it is wired, which is easy to read. Register simply opens `RegisterActivity` without clearing the stack, so Back returns here.
- **Step by step:**
  1. Login button → `attemptLogin()`.
  2. Register button → build an `Intent` to `RegisterActivity` and start it.
  3. Forgot-password button → `showForgotPasswordDialog()`.

### `protected void onCreate(Bundle savedInstanceState)`
```java
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
```
- **What it does:** Sets up the screen: inflates the layout, prepares the API client, skips straight to the dashboard if already signed in, and otherwise wires the buttons.
- **Where it's called:** Android calls it automatically when the Activity is created (the lifecycle entry point).
- **Why it's written this way:** The early `return` when a token exists keeps the login screen off the stack for returning users. `setDecorFitsSystemWindows(false)` gives the edge-to-edge look. Building `apiService` once here avoids recreating it per request.
- **Step by step:**
  1. Call `super.onCreate`.
  2. Tell the window to draw behind the system bars.
  3. Inflate `activity_login.xml` via ViewBinding and set it as the content view.
  4. Get the shared API service from `RetrofitClient`.
  5. If `TokenManager` already has a token, jump to the dashboard and return.
  6. Otherwise, wire up the click listeners.
