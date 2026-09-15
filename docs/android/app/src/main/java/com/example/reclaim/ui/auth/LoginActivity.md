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

What that function does: I show the login form, or I skip it if a token is already stored.
What the function is responsible for: I am the sign-in screen start.
Where it works: MainActivity starts me when there is no token. ResetPasswordActivity can also send the user here. I inflate activity_login.xml.
Why I chose to write the code of the function the way its written: I draw behind the system bars. I leave at once if already signed in so this screen does not sit on the stack.
More: I use ActivityLoginBinding, WindowCompat, RetrofitClient.getApiService, and TokenManager.hasToken.

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

What that function does: I wire login, go-to-register, and forgot-password.
What the function is responsible for: I am the click setup for the login form.
Where it works: I am called from onCreate. The views are in activity_login.xml.
Why I chose to write the code of the function the way its written: Lambdas keep the listeners next to the buttons.
More: I use Intent to RegisterActivity and showForgotPasswordDialog.

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

What that function does: I show a dialog that asks for an email, then I send a reset code.
What the function is responsible for: I am the forgot-password start from login.
Where it works: I am called from the forgot-password button. I inflate dialog_forgot_password.xml.
Why I chose to write the code of the function the way its written: I set the positive button listener in onShow so a bad email does not close the dialog. I copy the login email if it is already typed.
More: I use MaterialAlertDialogBuilder, Patterns.EMAIL_ADDRESS, and LayoutInflater.

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
                    Toast.makeText(LoginActivity.this,
                            R.string.error_generic, Toast.LENGTH_SHORT).show();
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

What that function does: I ask the server to email a reset code, then I open verify-otp in reset mode.
What the function is responsible for: I am the forgot-password HTTP call.
Where it works: I am called from showForgotPasswordDialog. Success starts VerifyOtpActivity with MODE_RESET.
Why I chose to write the code of the function the way its written: 404/400 stay on the dialog so an unknown email can be fixed. I re-enable buttons if the activity is still around.
More: I use ForgotPasswordRequest, ReClaimApiService.sendPasswordResetOtp, and ConnectionToasts.

```java
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
    Toast.makeText(LoginActivity.this,
            R.string.error_generic, Toast.LENGTH_SHORT).show();
}
```

What that function does: I handle the reset-code reply: open verify, show an email error, or a generic toast.
What the function is responsible for: I am the Retrofit success path for sendPasswordResetOtp.
Where it works: Retrofit calls me from sendPasswordResetOtp.
Why I chose to write the code of the function the way its written: isFinishing stops work after the user left. Unknown email stays in the dialog.
More: I use Response.code and VerifyOtpActivity extras.

```java
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
```

What that function does: I re-enable the dialog and toast a network error if the API is up.
What the function is responsible for: I am the Retrofit failure path for sendPasswordResetOtp.
Where it works: Retrofit calls me from sendPasswordResetOtp.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast when the offline dialog is the right message.
More: I use ConnectionToasts.showIfOnline.

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

What that function does: I check the form, then I call login and save the token on success.
What the function is responsible for: I am the sign-in submit path.
Where it works: I am called from the login button. Success opens DashboardActivity.
Why I chose to write the code of the function the way its written: I show field errors before the call. 401, 404, and 5xx get different toasts.
More: I use Patterns.EMAIL_ADDRESS, LoginRequest, apiService.login, and TokenManager.saveToken.

```java
public void onResponse(Call<LoginResponse> call,
                       Response<LoginResponse> response) {
    setLoadingState(false);

    if (response.isSuccessful() && response.body() != null) {
        String token = response.body().getToken();
        if (!TextUtils.isEmpty(token)) {
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
```

What that function does: I save the token and open home, or I show why sign-in failed.
What the function is responsible for: I am the Retrofit success path for login.
Where it works: Retrofit calls me from attemptLogin.
Why I chose to write the code of the function the way its written: An empty token is treated as a failure. Status codes map to clearer messages.
More: I use TokenManager.saveToken and LoginResponse.getToken.

```java
public void onFailure(Call<LoginResponse> call, Throwable t) {
    setLoadingState(false);
    ConnectionToasts.showIfOnline(LoginActivity.this,
            getString(R.string.msg_network_error, t.getLocalizedMessage()),
            Toast.LENGTH_LONG);
}
```

What that function does: I stop loading and toast a network error if the API is up.
What the function is responsible for: I am the Retrofit failure path for login.
Where it works: Retrofit calls me from attemptLogin.
Why I chose to write the code of the function the way its written: ConnectionToasts avoids a second message during an outage.
More: I use setLoadingState and ConnectionToasts.showIfOnline.

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

What that function does: I disable the form and change the button text while signing in.
What the function is responsible for: I stop a second submit during the request.
Where it works: I am called from attemptLogin and its callbacks.
Why I chose to write the code of the function the way its written: I also disable register and forgot-password so the user waits.
More: I toggle enabled flags and button text resources.

```java
private void navigateToDashboard() {
    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

What that function does: I open home and clear the back stack so Back will not return here.
What the function is responsible for: I am the post-login navigation.
Where it works: I am called after a saved token or a successful login.
Why I chose to write the code of the function the way its written: CLEAR_TASK drops login and register from Back.
More: I use Intent flags NEW_TASK and CLEAR_TASK.
