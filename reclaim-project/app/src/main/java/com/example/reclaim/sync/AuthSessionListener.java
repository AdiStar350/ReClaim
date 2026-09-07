package com.example.reclaim.sync;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.reclaim.data.PendingReportStore;

/**
 * Runs after a new login or sign-up.
 * Guest reports are claimed by this user and queued to send.
 */
public final class AuthSessionListener {

    private AuthSessionListener() {
    }

    /**
     * Called after a token is saved. Fine from the main thread.
     */
    public static void onAuthenticated(@NonNull Context context) {
        PendingReportStore.adoptUnassignedAndSync(context);
    }
}
