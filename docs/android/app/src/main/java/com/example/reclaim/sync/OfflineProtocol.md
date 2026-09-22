# OfflineProtocol.java

## Overview
This file is a **static helper** that shows one non-cancelable offline dialog when the
phone has no network or the backend API cannot be reached. The user can leave the app
or open the report screen to write a report that will send later. It tracks the
foreground **Activity** (one full screen; see `docs/GLOSSARY.md`) through lifecycle
callbacks so the dialog has a host without every screen registering itself.

## Concepts used here
- **Activity / Application.** The dialog needs a live Activity; `Application`
  lifecycle callbacks track which screen is in front; see `docs/GLOSSARY.md`.
- **HTTP / API.** "Backend unreachable" means a request could not reach the server;
  see `docs/GLOSSARY.md`.
- **Intent.** Used to open `ReportActivity` when the user taps Continue; see
  `docs/GLOSSARY.md`.
- **Static utility class.** Everything is static; the constructor is private.
- **AtomicBoolean.** Ensures only one dialog is shown at a time even if several
  callers race.

## Libraries and imports
- **Android (`Activity`, `Application`, `Intent`, `Bundle`, `Handler`, `Looper`).**
  Lifecycle registration, main-thread posting, and starting the report screen.
- **AndroidX annotations (`NonNull`, `Nullable`).** Mark required vs optional
  arguments.
- **Material (`MaterialAlertDialogBuilder`).** Builds the offline alert dialog.
- **Project classes.** `MainActivity` / `ReportActivity` — skipped or auto-acked so
  the splash and report flows are not interrupted; `R.string.*` — dialog copy;
  `BackendAvailability` / `NetworkMonitor` — decide if the API or device is down
  (used inside `showIfNeeded`).

## Fields
- `private static final Handler MAIN` — posts dialog work onto the main (UI) thread.
- `private static final AtomicBoolean dialogVisible` — `true` while a dialog is up;
  prevents stacking duplicates.
- `private static volatile Application application` — app object for online checks.
- `private static volatile Activity currentActivity` — the screen that can host the
  dialog.
- `private static volatile boolean outageAcknowledged` — once the user picks Exit or
  Continue (or is already on Report), the dialog stays hidden for this outage.

## Functions

### `private OfflineProtocol()`
```java
private OfflineProtocol() {
}
```
- **What it does:** empty private constructor blocking `new OfflineProtocol()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** the class is a static-only helper, so instances are
  not needed.
- **Step by step:** 1) private access; 2) empty body; 3) instances cannot be created
  from outside.

### `static void resetOutageAck()`
```java
static void resetOutageAck() {
    outageAcknowledged = false;
}
```
- **What it does:** clears the "user already dealt with this outage" flag.
- **Where it's called:** `BackendAvailability` when the backend is marked reachable
  again, so a later outage can show the dialog once more.
- **Why it's written this way:** package-private keeps the ack flag owned by the sync
  package; only availability code should reset it.
- **Step by step:** 1) set `outageAcknowledged` to `false`.

### `public static void attach(@NonNull Application app)`
```java
public static void attach(@NonNull Application app) {
    application = app;
    app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            currentActivity = activity;
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            if (currentActivity == activity) {
                currentActivity = null;
            }
        }

        @Override
        public void onActivityCreated(@NonNull Activity a, @Nullable Bundle s) { }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            currentActivity = activity;
        }

        @Override
        public void onActivityStopped(@NonNull Activity a) { }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle o) { }

        @Override
        public void onActivityDestroyed(@NonNull Activity a) { }
    });
}
```
- **What it does:** stores the `Application` and registers lifecycle callbacks that
  keep `currentActivity` pointing at the foreground screen.
- **Where it's called:** `ReclaimApp.onCreate` once at app start.
- **Why it's written this way:** one app-wide registration means screens do not each
  wire the offline dialog; start/resume update the host, pause clears it so a paused
  screen is never used.
- **Step by step:**
  1. Save `application`.
  2. Register `ActivityLifecycleCallbacks`.
  3. On started/resumed, set `currentActivity`.
  4. On paused, clear it only if it is still that same instance.
  5. Leave the other lifecycle methods empty.

### `public static void onBackendUnreachable()`
```java
public static void onBackendUnreachable() {
    MAIN.post(OfflineProtocol::showIfNeeded);
}
```
- **What it does:** schedules `showIfNeeded()` on the main thread when a request
  cannot reach the API.
- **Where it's called:** `RetrofitClient`'s OkHttp interceptor when the backend looks
  down.
- **Why it's written this way:** network callbacks may run off the UI thread; posting
  to `MAIN` is required before showing a dialog.
- **Step by step:** 1) post a runnable that calls `showIfNeeded()` onto the main
  looper.

### `public static void showIfNeeded(@NonNull Activity activity)`
```java
public static void showIfNeeded(@NonNull Activity activity) {
    currentActivity = activity;
    showIfNeeded();
}
```
- **What it does:** pins the given Activity as the current host, then runs the usual
  show logic.
- **Where it's called:** `DashboardActivity` when the dashboard comes up, so an
  ongoing outage is checked against that screen.
- **Why it's written this way:** an Activity that knows it is visible can force the
  host before the no-arg check runs.
- **Step by step:** 1) assign `currentActivity`; 2) call `showIfNeeded()`.

### `public static void showIfNeeded()`
```java
public static void showIfNeeded() {
    Activity activity = currentActivity;
    if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
        return;
    }
    if (activity instanceof MainActivity) {
        return;
    }
    if (outageAcknowledged) {
        return;
    }
    boolean backendDown = !BackendAvailability.isReachable();
    boolean deviceOffline = application != null && !NetworkMonitor.isOnline(application);
    if (!backendDown && !deviceOffline) {
        return;
    }
    if (activity instanceof ReportActivity) {
        outageAcknowledged = true;
        return;
    }
    if (!dialogVisible.compareAndSet(false, true)) {
        return;
    }

    new MaterialAlertDialogBuilder(activity)
            .setCancelable(false)
            .setMessage(R.string.offline_dialog_message)
            .setPositiveButton(R.string.offline_dialog_continue, (dialog, which) -> {
                outageAcknowledged = true;
                dialogVisible.set(false);
                activity.startActivity(new Intent(activity, ReportActivity.class));
            })
            .setNegativeButton(R.string.offline_dialog_exit, (dialog, which) -> {
                outageAcknowledged = true;
                dialogVisible.set(false);
                activity.finishAffinity();
            })
            .show();
}
```
- **What it does:** shows the offline dialog once if the API or device is down and the
  user has not already acknowledged the outage.
- **Where it's called:** from `onBackendUnreachable` (via the main handler), from
  `showIfNeeded(Activity)`, and indirectly whenever those paths run.
- **Why it's written this way:** many early returns avoid dialogs on splash, dying
  screens, or after the user already chose; on `ReportActivity` it only marks ack so
  report writing is not interrupted; `compareAndSet` stops duplicate dialogs.
- **Step by step:**
  1. Read `currentActivity`; bail if missing, finishing, or destroyed.
  2. Skip `MainActivity`; skip if already acknowledged.
  3. Check backend reachability and device online status; bail if both are fine.
  4. If already on `ReportActivity`, set ack and return without a dialog.
  5. Claim the dialog with `dialogVisible` (`false` → `true`); bail if another won.
  6. Build a non-cancelable Material dialog: Continue opens `ReportActivity` and
     clears the visible flag; Exit finishes the task affinity and clears the flag.
  7. Show the dialog.
