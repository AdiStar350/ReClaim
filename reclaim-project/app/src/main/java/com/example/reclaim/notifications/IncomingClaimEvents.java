package com.example.reclaim.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * App-wide note that a {@code CLAIM_PENDING} push arrived.
 * Home can show the claim card without waiting for {@code onResume}.
 */
public final class IncomingClaimEvents {

    private static final MutableLiveData<Long> SIGNAL = new MutableLiveData<>();

    private IncomingClaimEvents() {
    }

    public static LiveData<Long> live() {
        return SIGNAL;
    }

    /** Fine from any thread. */
    public static void notifyReceived() {
        SIGNAL.postValue(System.currentTimeMillis());
    }
}
