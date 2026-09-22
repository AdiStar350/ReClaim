# ProfileSettingsFragment.java

## Overview
This is the profile/settings **Fragment** (see `docs/GLOSSARY.md`). It shows a summary (name, email, avatar), and rows to edit profile, open report history, open system notification settings, start a password-reset OTP flow, log out, or delete the account. It is hosted by `DashboardActivity` (profile tab) and by `ProfileActivity`.

## Concepts used here
- **Fragment** — reusable settings UI inside a host Activity. See `docs/GLOSSARY.md`.
- **ViewBinding** — `FragmentProfileSettingsBinding`. See `docs/GLOSSARY.md`.
- **Intent** — opens Edit Profile, Report History, Verify OTP, Login. See `docs/GLOSSARY.md`.
- **Retrofit Call / Callback** — load user, send reset OTP, delete account. See `docs/GLOSSARY.md`.
- **JWT / SharedPreferences (TokenManager)** — auth header; clear on logout/delete. See `docs/GLOSSARY.md`.
- **OTP** — password update reuses the reset-code flow. See `docs/GLOSSARY.md`.
- **Glide** — round avatar. See `docs/GLOSSARY.md`.

## Libraries and imports
- **AndroidX**: Fragment, LayoutInflater, Settings intents.
- **Material**: MaterialAlertDialogBuilder, TextInputLayout/EditText for delete dialog.
- **Glide**: avatar load.
- **Retrofit**: Call, Callback, Response.
- **Project**: `EditProfileActivity`, `ReportHistoryActivity`, `LoginActivity`, `VerifyOtpActivity`, DTOs (`DeleteAccountRequest`, `ForgotPasswordRequest`), `TokenManager`, `AppDataRefresh`, `ConnectionToasts`.

## Fields
- `private FragmentProfileSettingsBinding binding` — settings views (nulled in `onDestroyView`).
- `private ReClaimApiService apiService` — API client.
- `private boolean showInitialProgress` — shows progress only on the first load, not every resume refresh.
- `private String currentEmail` — cached email for the password-update OTP request.

## Functions

### `private void loadAvatar(String url)`
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
- **What it does:** Loads a circular avatar from a URL if present.
- **Where it's called:** From `loadProfile` on successful user fetch.
- **Why it's written this way:** Leaves the default person icon when there is no URL.
- **Step by step:**
  1. Abort if binding/url missing.
  2. Clear padding/tint; Glide circle crop into the ImageView.

### `private void loadProfile()`
```java
private void loadProfile() {
    String authHeader = TokenManager.getAuthHeader(requireContext());
    if (authHeader == null) {
        return;
    }
    // optionally show progress; getCurrentUser → name, email, avatar
}
```
- **What it does:** Fetches the current user and fills the summary card.
- **Where it's called:** From `onResume` and `AppDataRefresh.observeNew` in `onViewCreated`.
- **Why it's written this way:** `showInitialProgress` avoids a flashing spinner on every quiet refresh.
- **Step by step:**
  1. Require auth header.
  2. Show progress only if `showInitialProgress`.
  3. Enqueue `getCurrentUser`; on success set name/email/`currentEmail` and `loadAvatar`.
  4. Hide progress; clear the initial-progress flag; on failure use `ConnectionToasts`.

### `private void openNotificationSettings()`
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
- **What it does:** Opens this app’s notification settings in the system UI, with a fallback to app details.
- **Where it's called:** Notifications row click in `onViewCreated`.
- **Why it's written this way:** Some devices lack the notification-settings action; the catch opens the generic app settings page.
- **Step by step:**
  1. Build notification-settings Intent with package extra.
  2. Try `startActivity`; on exception open `ACTION_APPLICATION_DETAILS_SETTINGS`.

### `private void startPasswordUpdate()`
```java
private void startPasswordUpdate() {
    if (TextUtils.isEmpty(currentEmail)) {
        Toast.makeText(requireContext(),
                R.string.msg_profile_load_failed, Toast.LENGTH_SHORT).show();
        loadProfile();
        return;
    }
    // sendPasswordResetOtp → VerifyOtpActivity MODE_RESET
}
```
- **What it does:** Emails a password-reset OTP for the loaded email and opens `VerifyOtpActivity` in reset mode.
- **Where it's called:** Password-update row click.
- **Why it's written this way:** Reuses the same OTP path as login’s forgot-password flow. If email is not loaded yet, it retries `loadProfile`.
- **Step by step:**
  1. If no `currentEmail`, toast and reload profile.
  2. Show progress; enqueue `sendPasswordResetOtp`.
  3. On success: toast, start Verify OTP with email + `MODE_RESET`.
  4. On 404/400/500/other: matching toast; on failure `ConnectionToasts`.

### `private void logout()`
```java
private void logout() {
    TokenManager.clearToken(requireContext());
    Intent intent = new Intent(requireContext(), LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
}
```
- **What it does:** Clears the saved JWT and opens Login with a cleared back stack.
- **Where it's called:** Logout row click, and after successful `deleteAccount`.
- **Why it's written this way:** Clearing the task prevents Back into authenticated screens.
- **Step by step:**
  1. `TokenManager.clearToken`.
  2. Start `LoginActivity` with NEW_TASK | CLEAR_TASK.

### `private void showDeleteAccountDialog()` / `private void deleteAccount(String name, String password)`
```java
private void showDeleteAccountDialog() {
    // inflate dialog_delete_account; require name+password; deleteAccount(...)
}

private void deleteAccount(String name, String password) {
    // deleteAccount API → clear token, toast, logout
}
```
- **What it does:** Confirms delete with name+password fields, then calls the delete-account API.
- **Where it's called:** Delete-account row → dialog; dialog positive → `deleteAccount`.
- **Why it's written this way:** Backend requires name and password confirmation so delete is intentional. Success clears the token and reuses `logout()` navigation.
- **Step by step:**
  1. Inflate dialog; on positive validate name/password errors.
  2. `deleteAccount`: show progress; enqueue `deleteAccount` with `DeleteAccountRequest`.
  3. On success: clear token, toast, `logout()`; on failure toast.

### `public View onCreateView(...)` / `public void onViewCreated(...)` / `onResume` / `onDestroyView`
```java
@Nullable
@Override
public View onCreateView(...) {
    binding = FragmentProfileSettingsBinding.inflate(inflater, container, false);
    return binding.getRoot();
}

@Override
public void onViewCreated(...) {
    apiService = RetrofitClient.getApiService();
    binding.textAppVersion.setText(getString(R.string.app_version_label, "1.0"));
    // wire row clicks; AppDataRefresh.observeNew → loadProfile
}

@Override
public void onResume() {
    super.onResume();
    loadProfile();
}

@Override
public void onDestroyView() {
    super.onDestroyView();
    binding = null;
}
```
- **What it does:** Inflates the layout, wires navigation/actions, reloads on resume, clears binding when the view is destroyed.
- **Where it's called:** Fragment lifecycle; host creates the Fragment in `DashboardActivity` (profile tab) or `ProfileActivity`.
- **Why it's written this way:** Nulling binding stops late API callbacks from touching destroyed views (`isAdded` checks elsewhere).
- **Step by step:**
  1. Inflate and return root.
  2. Set version label; wire profile/history/notifications/password/logout/delete clicks.
  3. Observe `AppDataRefresh` for reloads.
  4. `onResume` → `loadProfile`; `onDestroyView` → `binding = null`.
