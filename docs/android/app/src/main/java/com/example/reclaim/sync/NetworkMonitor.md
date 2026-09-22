# NetworkMonitor.java

## Overview
This file is a **singleton helper** that answers "does the phone have working internet?"
and watches for internet coming back. It wraps Android's `ConnectivityManager`, the
system service that reports network state. See `docs/GLOSSARY.md` for related terms.
Note the difference: this class checks the *device's* connection; whether the *backend*
answered is tracked separately in `BackendAvailability`.

## Concepts used here
- **ConnectivityManager.** The Android system service that reports the active network
  and its capabilities.
- **Network callback.** A listener the system calls when a network becomes available or
  is lost.
- **Capabilities check.** "Has internet" needs both the INTERNET capability and the
  VALIDATED capability (the network actually reached the internet, not just a Wi-Fi with
  a login page).
- **`synchronized` guard.** Registration happens only once, protected against being run
  twice.

## Libraries and imports
- **Android (`android.content.Context`, `android.net.ConnectivityManager`, `Network`,
  `NetworkCapabilities`, `android.util.Log`).** `getSystemService(...)` gets the
  manager; `getActiveNetwork()`/`getNetworkCapabilities(...)` read the current state;
  `registerDefaultNetworkCallback(...)` subscribes to changes.
- **AndroidX annotations (`androidx.annotation.NonNull`).** Documents non-null
  parameters.
- **Project classes.** `ReportSyncScheduler` (queue a sync when internet returns) and
  `BackendAvailability` (combine device + backend state).

## Fields
- `private static final String TAG` — the log tag `"NetworkMonitor"`.
- `private static boolean registered` — whether the network callback has already been
  registered, so it is not registered twice.

## Functions

### `public static boolean isOnline(@NonNull Context context)`
```java
public static boolean isOnline(@NonNull Context context) {
    ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
    if (manager == null) {
        return false;
    }
    Network network = manager.getActiveNetwork();
    if (network == null) {
        return false;
    }
    NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
    return capabilities != null
            && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
}
```
- **What it does:** returns `true` only if the device currently has a validated
  internet connection.
- **Where it's called:** `OfflineProtocol.showIfNeeded` (to decide "device offline"),
  and via `isBackendAvailable` below.
- **Why it's written this way:** it checks both INTERNET and VALIDATED so a Wi-Fi that is
  connected but cannot reach the internet (e.g. a captive portal) counts as offline. It
  returns `false` on any missing piece instead of throwing.
- **Step by step:**
  1. Get the `ConnectivityManager`; if null, return `false`.
  2. Get the active network; if none, return `false`.
  3. Get its capabilities; return `true` only if they include both INTERNET and
     VALIDATED.

### `public static synchronized void register(@NonNull Context context)`
```java
public static synchronized void register(@NonNull Context context) {
    if (registered) {
        return;
    }
    Context app = context.getApplicationContext();
    ConnectivityManager manager = app.getSystemService(ConnectivityManager.class);
    if (manager == null) {
        return;
    }
    manager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, "Network available; scheduling pending report sync");
            ReportSyncScheduler.enqueue(app);
        }

        @Override
        public void onLost(@NonNull Network network) {
            Log.d(TAG, "Network lost");
        }
    });
    registered = true;
}
```
- **What it does:** registers a one-time listener that schedules a report sync whenever
  a network with internet becomes available.
- **Where it's called:** `ReclaimApp.onCreate`, once at app startup.
- **Why it's written this way:** `synchronized` plus the `registered` flag guarantees the
  callback is added only once, even if called again. It uses the application context so
  the listener lives for the whole app. When connectivity returns, it enqueues the sync
  worker so queued offline reports go out automatically.
- **Step by step:**
  1. If already registered, return.
  2. Get the app context and the `ConnectivityManager`; if null, return.
  3. Register a default network callback whose `onAvailable` schedules a report sync and
     whose `onLost` just logs.
  4. Mark `registered = true`.

### `public static boolean isBackendAvailable(@NonNull Context context)`
```java
public static boolean isBackendAvailable(@NonNull Context context) {
    return isOnline(context) && BackendAvailability.isReachable();
}
```
- **What it does:** returns `true` only when the device is online *and* the last API
  call reached the server.
- **Where it's called:** `ConnectionToasts.showIfOnline`, `ReportActivity`,
  `ItemsViewModel`, and other screens that only show "online" behavior when the backend
  is really usable.
- **Why it's written this way:** it combines two facts — device connectivity and backend
  reachability — because the phone can have internet while the server is down. Both must
  be true to treat the app as truly online.
- **Step by step:** 1) check device internet; 2) AND with `BackendAvailability.isReachable()`.
