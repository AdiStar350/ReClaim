package com.example.reclaim.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Process-wide signal that a {@code CLAIM_PENDING} push arrived.
 * <p>
 * The home dashboard observes this so the high-priority incoming-claim
 * card can appear immediately while the app is already in the foreground,
 * without waiting for the next {@code onResume}.
 * </p>
 */
public final class IncomingClaimEvents {

    private static final MutableLiveData<Long> SIGNAL = new MutableLiveData<>();

    private IncomingClaimEvents() {
    }

    public static LiveData<Long> live() {
        return SIGNAL;
    }

    /** Posts from any thread (FCM callback). */
    public static void notifyReceived() {
        SIGNAL.postValue(System.currentTimeMillis());
    }
}
