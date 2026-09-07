package com.example.reclaim.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Checks if the device has internet, and watches for it coming back.
 */
public final class NetworkMonitor {

    private static final String TAG = "NetworkMonitor";
    private static boolean registered;

    private NetworkMonitor() {
    }

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

    /**
     * Queues report sync when a network with internet comes back.
     */
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

    /**
     * True when the device has internet and the last API call
     * reached the server.
     */
    public static boolean isBackendAvailable(@NonNull Context context) {
        return isOnline(context) && BackendAvailability.isReachable();
    }
}
