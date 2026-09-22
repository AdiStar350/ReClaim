# AuthSessionListener.java

## Overview
This file is a tiny **static hook** that runs after a new login or sign-up token is
saved. It tells the pending-report store to claim any guest (unsigned) offline reports
for the current user and queue them to send. Keeping that one call here means
`TokenManager` does not need to know about offline sync details.

## Concepts used here
- **Authentication.** A token was just saved, so the app now knows who the user is;
  see `docs/GLOSSARY.md`.
- **`Context`.** Needed to reach the database and WorkManager through the store; see
  `docs/GLOSSARY.md`.
- **WorkManager (indirect).** The store enqueues sync work after adopting rows; see
  `docs/GLOSSARY.md`.
- **Static utility class.** Private constructor; one public entry point.

## Libraries and imports
- **Android (`android.content.Context`).** Passed through to the store.
- **AndroidX annotations (`NonNull`).** The context must not be null.
- **Project class.** `PendingReportStore` — adopts unassigned pending reports and
  schedules sync.

## Fields
None.

## Functions

### `private AuthSessionListener()`
```java
private AuthSessionListener() {
}
```
- **What it does:** empty private constructor blocking `new AuthSessionListener()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** the class is a static-only hook, so instances are not
  needed.
- **Step by step:** 1) private access; 2) empty body; 3) instances cannot be created
  from outside.

### `public static void onAuthenticated(@NonNull Context context)`
```java
public static void onAuthenticated(@NonNull Context context) {
    PendingReportStore.adoptUnassignedAndSync(context);
}
```
- **What it does:** after a token is saved, adopts unassigned offline reports for this
  user and queues them to sync.
- **Where it's called:** `TokenManager.saveToken` right after the JWT is written to
  SharedPreferences (login, sign-up OTP, and reset-then-login paths).
- **Why it's written this way:** one named callback keeps "user just signed in" logic
  next to auth, while the heavy lifting stays in `PendingReportStore`. Safe to call
  from the main thread.
- **Step by step:** 1) call `PendingReportStore.adoptUnassignedAndSync(context)`.
