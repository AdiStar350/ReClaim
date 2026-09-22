# ConnectionToasts.java

## Overview
This file is a **static helper** that shows a Toast only when the backend API looks
reachable. When the API is down, the offline dialog (`OfflineProtocol`) already owns
the user message, so these toasts stay quiet to avoid stacking noisy popups.

## Concepts used here
- **Toast.** Short on-screen popup message; see `docs/GLOSSARY.md`.
- **`Context`.** Needed to create the Toast; see `docs/GLOSSARY.md`.
- **HTTP / API availability (indirect).** `NetworkMonitor.isBackendAvailable` decides
  whether the server is up enough to show a normal error toast; see HTTP in
  `docs/GLOSSARY.md`.
- **Static utility class.** Private constructor; two overloads.

## Libraries and imports
- **Android (`Context`, `Toast`).** Show the popup.
- **AndroidX annotations (`NonNull`, `StringRes`).** Required args; resource id for
  the string overload.
- **Project class.** `NetworkMonitor` — checks if the backend is considered available.

## Fields
None.

## Functions

### `private ConnectionToasts()`
```java
private ConnectionToasts() {
}
```
- **What it does:** empty private constructor blocking `new ConnectionToasts()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** static-only helper.
- **Step by step:** 1) private access; 2) empty body; 3) no instances from outside.

### `public static void showIfOnline(@NonNull Context context, @StringRes int resId, int duration)`
```java
public static void showIfOnline(@NonNull Context context, @StringRes int resId, int duration) {
    if (NetworkMonitor.isBackendAvailable(context)) {
        Toast.makeText(context, resId, duration).show();
    }
}
```
- **What it does:** shows a Toast from a string resource if the backend is available.
- **Where it's called:** many UI screens after failed or informational API calls, for
  example `LoginActivity`, `RegisterActivity`, `VerifyOtpActivity`,
  `ResetPasswordActivity`, `ReportActivity`, `ItemDetailsActivity`,
  `EditProfileActivity`, `PendingClaimsActivity`, `ProfileSettingsFragment`,
  `MyItemsFragment`, and related auth/profile flows.
- **Why it's written this way:** resource ids keep copy in `strings.xml`; gating on
  backend availability prevents toast spam during an outage the dialog already covers.
- **Step by step:** 1) if backend available, `Toast.makeText(...).show()` with
  `resId` and `duration`; 2) otherwise do nothing.

### `public static void showIfOnline(@NonNull Context context, @NonNull CharSequence message, int duration)`
```java
public static void showIfOnline(@NonNull Context context, @NonNull CharSequence message, int duration) {
    if (NetworkMonitor.isBackendAvailable(context)) {
        Toast.makeText(context, message, duration).show();
    }
}
```
- **What it does:** same gating, but with a ready-made message string (often from a
  server error body).
- **Where it's called:** screens that already built a dynamic message, such as
  `VerifyOtpActivity`, `SearchFragment`, `HomeFragment`, and some `ReportActivity` /
  `ItemDetailsActivity` paths.
- **Why it's written this way:** overload for runtime text without forcing every caller
  through a string resource.
- **Step by step:** 1) if backend available, show the Toast with `message`; 2)
  otherwise do nothing.
