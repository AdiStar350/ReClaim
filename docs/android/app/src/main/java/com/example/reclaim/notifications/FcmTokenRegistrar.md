# FcmTokenRegistrar.java

## Overview
This file is a **static helper** that sends this device's FCM (Firebase Cloud
Messaging) push token to the backend so the server can target this phone. It skips
work when the user is not signed in. Fine to call on every app start; failures are
logged and ignored so startup is not blocked.

## Concepts used here
- **Firebase / FCM.** Google push service; the token identifies this install; see
  `docs/GLOSSARY.md`.
- **JWT / auth header.** Token registration is an authenticated API call; see
  `docs/GLOSSARY.md`.
- **Retrofit `Call` / `Callback` / `enqueue`.** Async HTTP; see `docs/GLOSSARY.md`.
- **DTO.** `FcmTokenRequest` carries the token JSON body; see `docs/GLOSSARY.md`.
- **Static utility class.** Private constructor; all methods static.

## Libraries and imports
- **Android (`Context`, `Log`).** App context and failure logging.
- **AndroidX annotations (`NonNull`).** Required arguments.
- **Firebase Messaging (`FirebaseMessaging`).** Fetches the current FCM token.
- **Retrofit (`Call`, `Callback`, `Response`).** Async `updateFcmToken` call.
- **Project classes.** `FcmTokenRequest` — body; `RetrofitClient` — API; `TokenManager`
  — auth header / signed-in check.

## Fields
- `private static final String TAG = "FcmTokenRegistrar"` — log tag.

## Functions

### `private FcmTokenRegistrar()`
```java
private FcmTokenRegistrar() {
}
```
- **What it does:** empty private constructor blocking `new FcmTokenRegistrar()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** static-only helper.
- **Step by step:** 1) private access; 2) empty body; 3) no instances from outside.

### `public static void register(@NonNull Context context)`
```java
public static void register(@NonNull Context context) {
    Context app = context.getApplicationContext();
    if (TokenManager.getAuthHeader(app) == null) {
        return;
    }
    FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> sendToken(app, token))
            .addOnFailureListener(e -> Log.w(TAG, "Could not fetch FCM token", e));
}
```
- **What it does:** if signed in, asks Firebase for the current FCM token and, on
  success, sends it to the backend.
- **Where it's called:** `ReclaimApp.onCreate` and `DashboardActivity.onCreate` so the
  token is refreshed when the app or dashboard starts.
- **Why it's written this way:** early return when logged out avoids useless Firebase
  work; using application context avoids leaking an Activity; failures only log.
- **Step by step:**
  1. Take the application context.
  2. If there is no auth header, return.
  3. `getToken()`; on success call `sendToken`; on failure log a warning.

### `public static void sendToken(@NonNull Context context, @NonNull String token)`
```java
public static void sendToken(@NonNull Context context, @NonNull String token) {
    String authHeader = TokenManager.getAuthHeader(context);
    if (authHeader == null) {
        Log.d(TAG, "Not logged in; FCM token will be registered after login");
        return;
    }
    RetrofitClient.getApiService()
            .updateFcmToken(authHeader, new FcmTokenRequest(token))
            .enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call,
                                       @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "FCM token registered with backend");
                    } else {
                        Log.w(TAG, "FCM token registration failed http=" + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.w(TAG, "FCM token registration failed", t);
                }
            });
}
```
- **What it does:** POSTs the FCM token to the backend with the user's auth header, or
  skips if not logged in.
- **Where it's called:** from `register` after Firebase returns a token, and from
  `ReclaimMessagingService.onNewToken` when Firebase rotates the token.
- **Why it's written this way:** shared send path for both "fetch then register" and
  "token rotated"; fire-and-forget `enqueue` so UI/startup is not blocked; logs cover
  HTTP and network failures without crashing.
- **Step by step:**
  1. Read the auth header; if missing, log and return.
  2. Call `updateFcmToken` with `FcmTokenRequest(token)`.
  3. On response: log success or the HTTP code.
  4. On failure: log the throwable.
