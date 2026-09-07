package com.example.reclaim.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * App-wide note that server data changed.
 * Screens should also reload in {@code onResume}.
 */
public final class AppDataRefresh {

    private static final MutableLiveData<Long> SIGNAL = new MutableLiveData<>();

    private AppDataRefresh() {
    }

    public static LiveData<Long> live() {
        return SIGNAL;
    }

    /** Fine from any thread. */
    public static void notifyChanged() {
        SIGNAL.postValue(System.currentTimeMillis());
    }

    /**
     * Runs {@code onChanged} for ticks after this subscribe.
     * The current value is skipped so opening a screen does not fetch twice.
     */
    public static void observeNew(@NonNull LifecycleOwner owner, @NonNull Runnable onChanged) {
        final long baseline = currentTick();
        SIGNAL.observe(owner, tick -> {
            if (tick != null && tick > baseline) {
                onChanged.run();
            }
        });
    }

    private static long currentTick() {
        Long value = SIGNAL.getValue();
        return value != null ? value : 0L;
    }
}
