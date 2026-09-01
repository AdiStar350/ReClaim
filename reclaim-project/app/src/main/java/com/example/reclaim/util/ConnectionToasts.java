package com.example.reclaim.util;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.example.reclaim.sync.NetworkMonitor;

/**
 * Helpers for connection-related toasts.
 * <p>
 * When the device is offline the app surfaces a single global dialog on launch
 * rather than a stream of "network error" toasts on every screen change. These
 * helpers therefore only display a toast when the device is actually online,
 * suppressing redundant connection-error alerts while offline.
 * </p>
 */
public final class ConnectionToasts {

    private ConnectionToasts() {
    }

    /** Shows a connection-error toast only when the device is online. */
    public static void showIfOnline(@NonNull Context context, @StringRes int resId, int duration) {
        if (NetworkMonitor.isOnline(context)) {
            Toast.makeText(context, resId, duration).show();
        }
    }

    /** Shows a connection-error toast only when the device is online. */
    public static void showIfOnline(@NonNull Context context, @NonNull CharSequence message, int duration) {
        if (NetworkMonitor.isOnline(context)) {
            Toast.makeText(context, message, duration).show();
        }
    }
}
