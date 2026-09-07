package com.example.reclaim.sync;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.reclaim.MainActivity;
import com.example.reclaim.R;
import com.example.reclaim.ui.report.ReportActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Offline dialog: leave, or write a report that sends later.
 * Shown when the device or the API is down. One dialog per outage.
 */
public final class OfflineProtocol {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean dialogVisible = new AtomicBoolean(false);

    private static volatile Application application;
    private static volatile Activity currentActivity;
    private static volatile boolean outageAcknowledged;

    private OfflineProtocol() {
    }

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

    static void resetOutageAck() {
        outageAcknowledged = false;
    }

    /** Called when a request cannot reach the API. */
    public static void onBackendUnreachable() {
        MAIN.post(OfflineProtocol::showIfNeeded);
    }

    public static void showIfNeeded(@NonNull Activity activity) {
        currentActivity = activity;
        showIfNeeded();
    }

    /**
     * Shows the dialog if the API is down and the user
     * has not already picked Exit or Continue.
     */
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
}
