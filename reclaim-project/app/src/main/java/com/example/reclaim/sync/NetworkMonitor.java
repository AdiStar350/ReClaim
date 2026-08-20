package com.example.reclaim.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Reports validated internet availability and watches for reconnects.
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
     * Registers a process-wide callback that enqueues report sync whenever
     * a network with internet access becomes available.
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
}
