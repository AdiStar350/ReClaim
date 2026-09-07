package com.example.reclaim.util;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.example.reclaim.sync.NetworkMonitor;

/**
 * Toasts about the connection.
 * Shown only when the API is up; otherwise the offline dialog is used.
 */
public final class ConnectionToasts {

    private ConnectionToasts() {
    }

    /** Toast only if the API is up. */
    public static void showIfOnline(@NonNull Context context, @StringRes int resId, int duration) {
        if (NetworkMonitor.isBackendAvailable(context)) {
            Toast.makeText(context, resId, duration).show();
        }
    }

    /** Toast only if the API is up. */
    public static void showIfOnline(@NonNull Context context, @NonNull CharSequence message, int duration) {
        if (NetworkMonitor.isBackendAvailable(context)) {
            Toast.makeText(context, message, duration).show();
        }
    }
}
