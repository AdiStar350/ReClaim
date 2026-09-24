# TokenManager.java

## Overview
This file is a **singleton helper** that stores and reads the login token (a JWT) on
the phone. A JWT is a signed string the server gives you after login; the app sends it
back on later requests to prove it is signed in (see JWT in `docs/GLOSSARY.md`). The
token is kept in **SharedPreferences**, a tiny key–value store on the device (see
`docs/GLOSSARY.md`). Every static method here needs a `Context` because
SharedPreferences is reached through the Android app object.

## Concepts used here
- **JWT.** The login token this class saves and reads; see `docs/GLOSSARY.md`.
- **SharedPreferences.** The small on-device key–value store used to persist the
  token; see `docs/GLOSSARY.md`.
- **`Context`.** The Android "app right now" object needed to open SharedPreferences;
  see `docs/GLOSSARY.md`.
- **Auth header.** The value `"Bearer <token>"` sent in the `Authorization` header on
  signed-in requests.
- **Static utility class.** No fields on instances; everything is static and the
  constructor is private.

## Libraries and imports
- **Android (`android.content.Context`, `android.content.SharedPreferences`).**
  `context.getSharedPreferences(...)` opens the prefs file; `prefs.edit().putString().apply()`
  writes; `prefs.getString(...)` reads; `remove(...)` deletes.
- **AndroidX annotations (`androidx.annotation.NonNull`, `Nullable`).** Document which
  arguments/returns can be `null`, so callers handle the "no token" case.
- **Project classes.** `AuthSessionListener` — told after a token is saved so queued
  offline reports can be adopted; `JwtHelper` — reads the user id and expiry out of
  the token (used by `getUserId` and `hasToken`).

## Fields
- `private static final String PREFS_NAME = "reclaim_prefs"` — the name of the
  SharedPreferences file that holds the token.
- `private static final String KEY_TOKEN = "jwt_token"` — the key the token is stored
  under inside that file.

## Functions

### `private TokenManager()`
```java
private TokenManager() {
}
```
- **What it does:** empty private constructor blocking `new TokenManager()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** the class is a static-only helper, so instances are
  not needed.
- **Step by step:** 1) private access; 2) empty body; 3) instances cannot be created
  from outside.

### `public static void saveToken(@NonNull Context context, @NonNull String token)`
```java
public static void saveToken(@NonNull Context context, @NonNull String token) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    prefs.edit().putString(KEY_TOKEN, token).apply();
    AuthSessionListener.onAuthenticated(context);
}
```
- **What it does:** saves the login token to SharedPreferences and then notifies the
  auth-session listener.
- **Where it's called:** `LoginActivity` (after login), `VerifyOtpActivity` (after
  sign-up OTP), and `ResetPasswordActivity` (after a reset-then-login).
- **Why it's written this way:** saving the token and announcing "the user is now
  signed in" belong together, so any offline reports that were waiting for a login can
  be claimed and synced right away.
- **Step by step:**
  1. Open the private prefs file.
  2. Write the token under `KEY_TOKEN` and `apply()` (saves in the background).
  3. Call `AuthSessionListener.onAuthenticated(context)` so pending reports get
     adopted and a sync is queued.

### `public static String getToken(@NonNull Context context)`
```java
@Nullable
public static String getToken(@NonNull Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    return prefs.getString(KEY_TOKEN, null);
}
```
- **What it does:** returns the saved token, or `null` if none is stored.
- **Where it's called:** inside this class by `getAuthHeader`, `hasToken`, and
  `getUserId`. It is the low-level read the other methods build on.
- **Why it's written this way:** one small reader keeps the prefs key in a single
  place; the `null` default clearly means "not signed in."
- **Step by step:** 1) open the prefs file; 2) read the string at `KEY_TOKEN`, with
  `null` as the default; 3) return it.

### `public static String getAuthHeader(@NonNull Context context)`
```java
@Nullable
public static String getAuthHeader(@NonNull Context context) {
    String token = getToken(context);
    return token != null ? "Bearer " + token : null;
}
```
- **What it does:** returns the ready-to-send header value `"Bearer <token>"`, or
  `null` if there is no token.
- **Where it's called:** in almost every networked screen and worker (for example
  `HomeFragment`, `ItemDetailsActivity`, `MyItemsFragment`, `ReportActivity`,
  `EditProfileActivity`, `ProfileSettingsFragment`,
  `ReportHistoryActivity`, `ReportSyncWorker`, and `FcmTokenRegistrar`) to fill the
  `@Header("Authorization")` argument on API calls.
- **Why it's written this way:** it hides the exact header format ("Bearer " prefix)
  so callers do not repeat it; returning `null` lets callers skip a call when signed
  out.
- **Step by step:** 1) read the token; 2) if present, prefix it with `"Bearer "`; 3)
  otherwise return `null`.

### `public static void clearToken(@NonNull Context context)`
```java
public static void clearToken(@NonNull Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    prefs.edit().remove(KEY_TOKEN).apply();
}
```
- **What it does:** deletes the saved token (used to log out).
- **Where it's called:** `ProfileSettingsFragment` on log out and after account
  deletion; also internally by `hasToken` when a bad or expired token is found.
- **Why it's written this way:** removing just the token key is a clean, targeted log
  out without wiping other preferences.
- **Step by step:** 1) open the prefs file; 2) remove the `KEY_TOKEN` entry; 3)
  `apply()`.

### `public static boolean hasToken(@NonNull Context context)`
```java
public static boolean hasToken(@NonNull Context context) {
    String token = getToken(context);
    if (token == null || token.isEmpty() || JwtHelper.isExpired(token)) {
        if (token != null) {
            clearToken(context);
        }
        return false;
    }
    return true;
}
```
- **What it does:** returns `true` only if a good, non-expired token is stored; if the
  token is empty or expired it deletes it and returns `false`.
- **Where it's called:** `MainActivity` and `LoginActivity` at startup, to decide
  whether to send the user straight to the dashboard or to the login screen.
- **Why it's written this way:** checking validity here (not just presence) prevents
  the app from acting "logged in" with a token the server would reject; clearing the
  bad token keeps the stored state honest.
- **Step by step:**
  1. Read the token.
  2. If it is missing, empty, or expired (checked by `JwtHelper.isExpired`): if a
     token value exists, clear it, then return `false`.
  3. Otherwise return `true`.

### `public static String getUserId(@NonNull Context context)`
```java
@Nullable
public static String getUserId(@NonNull Context context) {
    return JwtHelper.getUserId(getToken(context));
}
```
- **What it does:** returns the current user's id, read out of the saved token, or
  `null`.
- **Where it's called:** `HomeFragment`, `ItemsViewModel`, `ItemDetailsActivity`,
  `EditProfileActivity`, `ReportActivity`, `PendingReportStore.adoptUnassignedAndSync`,
  and `ReportSyncWorker` — anywhere the app needs to know "who am I" without another
  network call.
- **Why it's written this way:** the id is already inside the token, so reading it
  locally avoids an extra request; it delegates the decoding to `JwtHelper`.
- **Step by step:** 1) read the token; 2) pass it to `JwtHelper.getUserId`, which
  decodes the token and returns the `sub` (subject) claim.
