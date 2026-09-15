```java
private AppDataRefresh() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep this class as static methods only.
Where it works: I am not called. Java uses me when someone tries `new AppDataRefresh()`.
Why I chose to write the code of the function the way its written: I made the constructor private so screens share one signal.
More: I use a private constructor. The class is final.

```java
public static LiveData<Long> live() {
    return SIGNAL;
}
```

What that function does: I return the shared refresh clock.
What the function is responsible for: I let a dialog observe ticks and stop when it closes.
Where it works: I am called by ContactInfoDialog.
Why I chose to write the code of the function the way its written: I return LiveData so only this class can post.
More: I use MutableLiveData SIGNAL.

```java
public static void notifyChanged() {
    SIGNAL.postValue(System.currentTimeMillis());
}
```

What that function does: I tick that server data changed.
What the function is responsible for: I tell lists to reload.
Where it works: I am called by AppStateEvents.notifyMutation. Screens also reload in onResume.
Why I chose to write the code of the function the way its written: I use postValue so a background thread can call me.
More: I use LiveData.postValue with the current time.

```java
public static void observeNew(@NonNull LifecycleOwner owner, @NonNull Runnable onChanged) {
    final long baseline = currentTick();
    SIGNAL.observe(owner, tick -> {
        if (tick != null && tick > baseline) {
            onChanged.run();
        }
    });
}
```

What that function does: I run onChanged only for ticks after this subscribe.
What the function is responsible for: I refresh a screen when data changes, without a double fetch on open.
Where it works: I am called by SearchFragment, ProfileSettingsFragment, ReportHistoryActivity, MyItemsFragment, and PendingClaimsActivity.
Why I chose to write the code of the function the way its written: I skip the current value so opening a screen does not load twice.
More: I use LifecycleOwner.observe and currentTick.

```java
private static long currentTick() {
    Long value = SIGNAL.getValue();
    return value != null ? value : 0L;
}
```

What that function does: I return the last posted time, or 0 if none yet.
What the function is responsible for: I give observeNew a baseline.
Where it works: I am called by observeNew.
Why I chose to write the code of the function the way its written: A 0 baseline is safe before the first post.
More: I use LiveData.getValue.
