```java
@Nullable
@Override
public View onCreateView(@NonNull LayoutInflater inflater,
                         @Nullable ViewGroup container,
                         @Nullable Bundle savedInstanceState) {
    binding = FragmentProfileSettingsBinding.inflate(inflater, container, false);
    return binding.getRoot();
}
```

What that function does: I inflate the settings layout.
What the function is responsible for: I am the fragment view create.
Where it works: DashboardActivity and ProfileActivity host me. I inflate fragment_profile_settings.xml.
Why I chose to write the code of the function the way its written: View Binding keeps ids typed.
More: I use FragmentProfileSettingsBinding.inflate.

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    apiService = RetrofitClient.getApiService();
    binding.textAppVersion.setText(getString(R.string.app_version_label, "1.0"));

    binding.cardProfile.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), EditProfileActivity.class)));
    binding.rowReportHistory.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), ReportHistoryActivity.class)));
    binding.rowNotifications.setOnClickListener(v -> openNotificationSettings());
    binding.rowPasswordUpdate.setOnClickListener(v -> startPasswordUpdate());
    binding.rowLogout.setOnClickListener(v -> logout());
    binding.rowDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    AppDataRefresh.observeNew(getViewLifecycleOwner(), this::loadProfile);
}
```

What that function does: I wire the settings rows and show the app version.
What the function is responsible for: I am the settings click setup.
Where it works: Fragment calls me after onCreateView.
Why I chose to write the code of the function the way its written: Edit lives on EditProfileActivity so this screen stays a menu.
More: I use Intents to EditProfileActivity and ReportHistoryActivity.

```java
@Override
public void onResume() {
    super.onResume();
    loadProfile();
}
```

What that function does: I reload the summary when this tab comes back.
What the function is responsible for: I pick up name and photo after EditProfileActivity.
Where it works: Android calls me. AppDataRefresh also reloads.
Why I chose to write the code of the function the way its written: onResume covers returning from edit.
More: I call loadProfile.

```java
private void loadProfile() {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) {
        return;
    }

    if (showInitialProgress) {
        binding.progress.setVisibility(View.VISIBLE);
    }
    apiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
        @Override
        public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
            if (binding == null || !isAdded()) {
                return;
            }
            binding.progress.setVisibility(View.GONE);
            showInitialProgress = false;
            if (response.isSuccessful() && response.body() != null) {
                User user = response.body();
                if (user.getName() != null) {
                    binding.textUserDisplayName.setText(user.getName());
                }
                if (user.getEmail() != null) {
                    currentEmail = user.getEmail();
                    binding.textUserDisplayEmail.setText(user.getEmail());
                }
                loadAvatar(user.getProfilePictureUrl());
            }
        }

        @Override
        public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
            if (binding == null || !isAdded()) {
                return;
            }
            binding.progress.setVisibility(View.GONE);
            showInitialProgress = false;
            ConnectionToasts.showIfOnline(requireContext(),
                    R.string.msg_profile_load_failed, Toast.LENGTH_SHORT);
        }
    });
}
```

What that function does: I fetch /users/me and fill name, email, and avatar.
What the function is responsible for: I am the profile summary load.
Where it works: I am called from onResume and AppDataRefresh.
Why I chose to write the code of the function the way its written: I only show the spinner the first time so later refreshes stay quiet. currentEmail is kept for password update.
More: I use apiService.getCurrentUser.

```java
public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    showInitialProgress = false;
    if (response.isSuccessful() && response.body() != null) {
        User user = response.body();
        if (user.getName() != null) {
            binding.textUserDisplayName.setText(user.getName());
        }
        if (user.getEmail() != null) {
            currentEmail = user.getEmail();
            binding.textUserDisplayEmail.setText(user.getEmail());
        }
        loadAvatar(user.getProfilePictureUrl());
    }
}
```

What that function does: I fill the summary from the user reply.
What the function is responsible for: I am the Retrofit success path for getCurrentUser.
Where it works: Retrofit calls me from loadProfile.
Why I chose to write the code of the function the way its written: Null name or email leaves whatever was already on screen.
More: I call loadAvatar.

```java
public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    showInitialProgress = false;
    ConnectionToasts.showIfOnline(requireContext(),
            R.string.msg_profile_load_failed, Toast.LENGTH_SHORT);
}
```

What that function does: I hide the spinner and toast if the API is up.
What the function is responsible for: I am the Retrofit failure path for getCurrentUser.
Where it works: Retrofit calls me from loadProfile.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline.

```java
private void loadAvatar(@Nullable String url) {
    if (binding == null || url == null || url.isEmpty()) {
        return;
    }
    binding.imageSettingsAvatar.setPadding(0, 0, 0, 0);
    binding.imageSettingsAvatar.setImageTintList(null);
    Glide.with(this)
            .load(url)
            .circleCrop()
            .placeholder(R.drawable.ic_settings_person)
            .error(R.drawable.ic_settings_person)
            .into(binding.imageSettingsAvatar);
}
```

What that function does: I show the profile photo as a round avatar.
What the function is responsible for: I load the settings avatar.
Where it works: I am called from loadProfile.
Why I chose to write the code of the function the way its written: Empty URL keeps the placeholder icon. I clear padding and tint like the other avatar screens.
More: I use Glide.circleCrop.

```java
private void openNotificationSettings() {
    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
    try {
        startActivity(intent);
    } catch (Exception e) {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:" + requireContext().getPackageName())));
    }
}
```

What that function does: I open this app's notification settings, or app details if that intent fails.
What the function is responsible for: I am the notifications row.
Where it works: I am called from rowNotifications.
Why I chose to write the code of the function the way its written: Some devices lack ACTION_APP_NOTIFICATION_SETTINGS, so I fall back.
More: I use Settings.EXTRA_APP_PACKAGE.

```java
private void startPasswordUpdate() {
    if (TextUtils.isEmpty(currentEmail)) {
        Toast.makeText(requireContext(),
                R.string.msg_profile_load_failed, Toast.LENGTH_SHORT).show();
        loadProfile();
        return;
    }

    final String email = currentEmail;
    binding.progress.setVisibility(View.VISIBLE);
    apiService.sendPasswordResetOtp(new ForgotPasswordRequest(email))
            .enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call,
                                       @NonNull Response<Void> response) {
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    binding.progress.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(),
                                R.string.msg_reset_code_sent, Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(requireContext(),
                                VerifyOtpActivity.class);
                        intent.putExtra(VerifyOtpActivity.EXTRA_EMAIL, email);
                        intent.putExtra(VerifyOtpActivity.EXTRA_MODE,
                                VerifyOtpActivity.MODE_RESET);
                        startActivity(intent);
                        return;
                    }
                    int message = (response.code() == 404 || response.code() == 400)
                            ? R.string.msg_no_account_for_email
                            : R.string.error_generic;
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    binding.progress.setVisibility(View.GONE);
                    ConnectionToasts.showIfOnline(requireContext(),
                            getString(R.string.msg_network_error, t.getLocalizedMessage()),
                            Toast.LENGTH_LONG);
                }
            });
}
```

What that function does: I email a reset code to the signed-in address and open verify in reset mode.
What the function is responsible for: I am the in-app password update start.
Where it works: I am called from rowPasswordUpdate.
Why I chose to write the code of the function the way its written: I reuse the same reset flow as login so I do not build a second path.
More: I use ForgotPasswordRequest and VerifyOtpActivity.MODE_RESET.

```java
public void onResponse(@NonNull Call<Void> call,
                       @NonNull Response<Void> response) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    if (response.isSuccessful()) {
        Toast.makeText(requireContext(),
                R.string.msg_reset_code_sent, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(requireContext(),
                VerifyOtpActivity.class);
        intent.putExtra(VerifyOtpActivity.EXTRA_EMAIL, email);
        intent.putExtra(VerifyOtpActivity.EXTRA_MODE,
                VerifyOtpActivity.MODE_RESET);
        startActivity(intent);
        return;
    }
    int message = (response.code() == 404 || response.code() == 400)
            ? R.string.msg_no_account_for_email
            : R.string.error_generic;
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
}
```

What that function does: I open verify-otp, or I toast why the code was not sent.
What the function is responsible for: I am the Retrofit success path for sendPasswordResetOtp from settings.
Where it works: Retrofit calls me from startPasswordUpdate.
Why I chose to write the code of the function the way its written: 404/400 mean the email on the profile did not match an account.
More: I use VerifyOtpActivity extras.

```java
public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    ConnectionToasts.showIfOnline(requireContext(),
            getString(R.string.msg_network_error, t.getLocalizedMessage()),
            Toast.LENGTH_LONG);
}
```

What that function does: I hide the spinner and toast a network error if the API is up.
What the function is responsible for: I am the Retrofit failure path for sendPasswordResetOtp from settings.
Where it works: Retrofit calls me from startPasswordUpdate.
Why I chose to write the code of the function the way its written: ConnectionToasts hides the toast during an outage.
More: I use ConnectionToasts.showIfOnline.

```java
private void logout() {
    TokenManager.clearToken(requireContext());
    Intent intent = new Intent(requireContext(), LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
}
```

What that function does: I drop the token and open login with a clear back stack.
What the function is responsible for: I am sign-out, and the post-delete send-off.
Where it works: I am called from rowLogout and from a successful deleteAccount.
Why I chose to write the code of the function the way its written: CLEAR_TASK keeps the user from backing into settings.
More: I use TokenManager.clearToken.

```java
private void showDeleteAccountDialog() {
    View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_account, null);
    TextInputLayout nameLayout = dialogView.findViewById(R.id.input_layout_confirm_name);
    TextInputLayout passwordLayout = dialogView.findViewById(R.id.input_layout_confirm_password);
    TextInputEditText nameInput = dialogView.findViewById(R.id.edit_confirm_name);
    TextInputEditText passwordInput = dialogView.findViewById(R.id.edit_confirm_password);

    new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_account_title)
            .setView(dialogView)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete_account, (d, w) -> {
                String name = nameInput.getText() != null
                        ? nameInput.getText().toString().trim() : "";
                String password = passwordInput.getText() != null
                        ? passwordInput.getText().toString() : "";
                if (TextUtils.isEmpty(name)) {
                    nameLayout.setError(getString(R.string.msg_name_required));
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    passwordLayout.setError(getString(R.string.msg_password_required));
                    return;
                }
                deleteAccount(name, password);
            })
            .show();
}
```

What that function does: I ask for name and password before delete.
What the function is responsible for: I am the delete confirm dialog.
Where it works: I am called from rowDeleteAccount. I inflate dialog_delete_account.xml.
Why I chose to write the code of the function the way its written: The server checks name and password, so I collect both here. Empty fields set layout errors.
More: I use MaterialAlertDialogBuilder.

```java
private void deleteAccount(String name, String password) {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) {
        return;
    }

    binding.progress.setVisibility(View.VISIBLE);
    apiService.deleteAccount(authHeader, new DeleteAccountRequest(name, password))
            .enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    binding.progress.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        TokenManager.clearToken(requireContext());
                        Toast.makeText(requireContext(),
                                R.string.msg_account_deleted, Toast.LENGTH_SHORT).show();
                        logout();
                    } else {
                        Toast.makeText(requireContext(),
                                R.string.msg_delete_account_failed, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    binding.progress.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            R.string.msg_delete_account_failed, Toast.LENGTH_SHORT).show();
                }
            });
}
```

What that function does: I DELETE the account, then I sign out.
What the function is responsible for: I am DELETE /api/users/me from settings.
Where it works: I am called from showDeleteAccountDialog.
Why I chose to write the code of the function the way its written: I clear the token on success before logout so the next screen is login.
More: I use DeleteAccountRequest and apiService.deleteAccount.

```java
public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    if (response.isSuccessful()) {
        TokenManager.clearToken(requireContext());
        Toast.makeText(requireContext(),
                R.string.msg_account_deleted, Toast.LENGTH_SHORT).show();
        logout();
    } else {
        Toast.makeText(requireContext(),
                R.string.msg_delete_account_failed, Toast.LENGTH_SHORT).show();
    }
}
```

What that function does: I sign out after delete, or I toast that delete failed.
What the function is responsible for: I am the Retrofit success path for deleteAccount.
Where it works: Retrofit calls me from deleteAccount.
Why I chose to write the code of the function the way its written: Wrong name or password comes back as a failed response, not a network error.
More: I call logout.

```java
public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
    if (binding == null || !isAdded()) {
        return;
    }
    binding.progress.setVisibility(View.GONE);
    Toast.makeText(requireContext(),
            R.string.msg_delete_account_failed, Toast.LENGTH_SHORT).show();
}
```

What that function does: I hide the spinner and toast that delete failed.
What the function is responsible for: I am the Retrofit failure path for deleteAccount.
Where it works: Retrofit calls me from deleteAccount.
Why I chose to write the code of the function the way its written: A simple failed toast is enough here.
More: I use msg_delete_account_failed.

```java
@Override
public void onDestroyView() {
    super.onDestroyView();
    binding = null;
}
```

What that function does: I drop the binding when the view is destroyed.
What the function is responsible for: I stop late callbacks from touching a dead view.
Where it works: Fragment calls me. loadProfile checks binding == null.
Why I chose to write the code of the function the way its written: View Binding docs ask to null the binding here.
More: I set binding to null.
