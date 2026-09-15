```java
private NetworkMonitor() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep network checks as static methods.
Where it works: I am not called. Java uses me when someone tries `new NetworkMonitor()`.
Why I chose to write the code of the function the way its written: I made the constructor private so ReclaimApp calls register.
More: I use a private constructor. The class is final.

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

What that function does: I return true when the device has a validated internet network.
What the function is responsible for: I am the device-online check, not the API check.
Where it works: I am called by isBackendAvailable and OfflineProtocol.showIfNeeded.
Why I chose to write the code of the function the way its written: VALIDATED means the network actually has internet, not just Wi-Fi with no path.
More: I use ConnectivityManager.getActiveNetwork and NetworkCapabilities.

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

What that function does: I watch for a network coming back, once per process.
What the function is responsible for: I start queued report send when internet returns.
Where it works: I am called by ReclaimApp.onCreate.
Why I chose to write the code of the function the way its written: synchronized plus a registered flag stops a second callback. I use the app context so the callback outlives an activity.
More: I use ConnectivityManager.registerDefaultNetworkCallback.

```java
public void onAvailable(@NonNull Network network) {
    Log.d(TAG, "Network available; scheduling pending report sync");
    ReportSyncScheduler.enqueue(app);
}
```

What that function does: I queue a report sync when a network becomes available.
What the function is responsible for: I am the "back online" hook.
Where it works: ConnectivityManager calls me from the callback registered in register.
Why I chose to write the code of the function the way its written: I enqueue even if the user is mid-screen so drafts send without a tap.
More: I call ReportSyncScheduler.enqueue with the application context.

```java
public void onLost(@NonNull Network network) {
    Log.d(TAG, "Network lost");
}
```

What that function does: I log that the network was lost.
What the function is responsible for: I fill the callback. The offline dialog comes from failed HTTP, not from this.
Where it works: ConnectivityManager calls me from the callback registered in register.
Why I chose to write the code of the function the way its written: A lost network is noisy, so I only log. Retrofit marks the API down on the next failed call.
More: I use Log.d.

```java
public static boolean isBackendAvailable(@NonNull Context context) {
    return isOnline(context) && BackendAvailability.isReachable();
}
```

What that function does: I return true when the device is online and the last API call reached the server.
What the function is responsible for: I gate toasts and whether ReportActivity should queue instead of POST.
Where it works: I am called by ConnectionToasts, ReportActivity, and ItemsViewModel.
Why I chose to write the code of the function the way its written: Device internet and API up are different, so I require both.
More: I use isOnline and BackendAvailability.isReachable.
