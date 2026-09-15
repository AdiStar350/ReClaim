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

What that function does: I show the new-password form if email and reset token arrived.
What the function is responsible for: I am the last forgot-password screen.
Where it works: VerifyOtpActivity starts me. I inflate activity_reset_password.xml.
Why I chose to write the code of the function the way its written: Missing extras close me so this screen cannot run without a token.
More: I use ActivityResetPasswordBinding, EXTRA_EMAIL, and EXTRA_RESET_TOKEN.

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

What that function does: I check the two password fields, then I save the new password.
What the function is responsible for: I am the update-password submit path.
Where it works: I am called from btnUpdatePassword. Success calls loginWithNewPassword.
Why I chose to write the code of the function the way its written: Confirm must match before the call. A failed reset stays on this screen.
More: I use ResetPasswordRequest and apiService.resetPassword.

```java
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
```

What that function does: I sign in with the new password, or I show that the reset token failed.
What the function is responsible for: I am the Retrofit success path for resetPassword.
Where it works: Retrofit calls me from attemptReset.
Why I chose to write the code of the function the way its written: I keep loading on while login runs so the button stays locked.
More: I call loginWithNewPassword.

```java
public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
    setLoading(false);
    ConnectionToasts.showIfOnline(ResetPasswordActivity.this,
            getString(R.string.msg_network_error, t.getLocalizedMessage()),
            Toast.LENGTH_LONG);
}
```

What that function does: I stop loading and toast a network error if the API is up.
What the function is responsible for: I am the Retrofit failure path for resetPassword.
Where it works: Retrofit calls me from attemptReset.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline.

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

What that function does: I sign in with the new password so I get a real token.
What the function is responsible for: I finish reset by opening a session.
Where it works: I am called after resetPassword succeeds.
Why I chose to write the code of the function the way its written: Reset does not return a login token, so I call login. If that fails I send the user to LoginActivity.
More: I use LoginRequest, TokenManager.saveToken, and LoginResponse.

```java
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
```

What that function does: I save the token and open home, or I send the user back to login.
What the function is responsible for: I am the Retrofit success path for the follow-up login.
Where it works: Retrofit calls me from loginWithNewPassword.
Why I chose to write the code of the function the way its written: The password is already changed, so a failed auto-login should still let them sign in by hand.
More: I use TokenManager.saveToken.

```java
public void onFailure(@NonNull Call<LoginResponse> call,
                      @NonNull Throwable t) {
    setLoading(false);
    ConnectionToasts.showIfOnline(ResetPasswordActivity.this,
            getString(R.string.msg_network_error, t.getLocalizedMessage()),
            Toast.LENGTH_LONG);
    navigateToLogin();
}
```

What that function does: I toast a network error if online, then I open login.
What the function is responsible for: I am the Retrofit failure path for the follow-up login.
Where it works: Retrofit calls me from loginWithNewPassword.
Why I chose to write the code of the function the way its written: The password was saved, so login is the fallback.
More: I use ConnectionToasts and navigateToLogin.

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

What that function does: I show a spinner and lock the form while reset or login runs.
What the function is responsible for: I stop a second submit.
Where it works: I am called from attemptReset and the login callbacks.
Why I chose to write the code of the function the way its written: The button text switches to "updating".
More: I toggle progress and enabled flags.

```java
private void navigateToDashboard() {
    Intent intent = new Intent(this, DashboardActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

What that function does: I open home and clear the back stack.
What the function is responsible for: I am post-reset navigation after auto-login.
Where it works: I am called after loginWithNewPassword succeeds.
Why I chose to write the code of the function the way its written: CLEAR_TASK drops the reset flow from Back.
More: I use Intent flags NEW_TASK and CLEAR_TASK.

```java
private void navigateToLogin() {
    Intent intent = new Intent(this, LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

What that function does: I open login and clear the back stack.
What the function is responsible for: I am the fallback when auto-login fails after a password change.
Where it works: I am called from loginWithNewPassword failure paths.
Why I chose to write the code of the function the way its written: CLEAR_TASK drops verify and reset so the user starts sign-in clean.
More: I use Intent flags NEW_TASK and CLEAR_TASK.
