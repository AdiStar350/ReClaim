package com.example.reclaim.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * App-wide note that item or claim data just changed.
 * Screens can refresh, and details can keep a local "already acted" flag.
 */
public final class AppStateEvents {

    private static final MutableLiveData<Long> SIGNAL = new MutableLiveData<>();
    private static final Set<String> INTERACTED_ITEM_IDS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AppStateEvents() {
    }

    public static LiveData<Long> live() {
        return SIGNAL;
    }

    /** Fine from any thread. */
    public static void notifyMutation() {
        SIGNAL.postValue(System.currentTimeMillis());
        AppDataRefresh.notifyChanged();
    }

    /**
     * Remembers that this viewer already acted on the item,
     * then tells the rest of the app.
     */
    public static void markItemInteracted(@Nullable String itemId) {
        if (!TextUtils.isEmpty(itemId)) {
            INTERACTED_ITEM_IDS.add(itemId);
        }
        notifyMutation();
    }

    public static boolean wasItemInteracted(@Nullable String itemId) {
        return !TextUtils.isEmpty(itemId) && INTERACTED_ITEM_IDS.contains(itemId);
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
