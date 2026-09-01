package com.example.reclaim.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Process-wide signal that a {@code CLAIM_DECISION} push arrived, so the
 * claimant's home dashboard can show the decision card immediately.
 */
public final class ClaimDecisionEvents {

    private static final MutableLiveData<Long> SIGNAL = new MutableLiveData<>();

    private ClaimDecisionEvents() {
    }

    public static LiveData<Long> live() {
        return SIGNAL;
    }

    public static void notifyReceived() {
        SIGNAL.postValue(System.currentTimeMillis());
    }
}
