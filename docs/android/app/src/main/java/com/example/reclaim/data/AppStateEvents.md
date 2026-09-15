```java
private AppStateEvents() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep this class as static methods only.
Where it works: I am not called. Java uses me when someone tries `new AppStateEvents()`.
Why I chose to write the code of the function the way its written: I made the constructor private so screens share one signal.
More: I use a private constructor. The class is final.

```java
public static LiveData<Long> live() {
    return SIGNAL;
}
```

What that function does: I return the shared mutation clock.
What the function is responsible for: I let callers observe ticks if they want the raw LiveData.
Where it works: I did not find an app caller of live() on this class. Screens use observeNew instead.
Why I chose to write the code of the function the way its written: I return LiveData, not MutableLiveData, so only this class can post.
More: I use MutableLiveData SIGNAL.

```java
public static void notifyMutation() {
    SIGNAL.postValue(System.currentTimeMillis());
    AppDataRefresh.notifyChanged();
}
```

What that function does: I tick that item or claim data changed, and I also tick AppDataRefresh.
What the function is responsible for: I tell lists and details to reload after a write or a push.
Where it works: I am called by RetrofitClient.interceptBackendAvailability, ReclaimMessagingService, and markItemInteracted.
Why I chose to write the code of the function the way its written: I use postValue so a background interceptor can call me. I also notify AppDataRefresh so older screens still refresh.
More: I use LiveData.postValue and AppDataRefresh.notifyChanged.

```java
public static void markItemInteracted(@Nullable String itemId) {
    if (!TextUtils.isEmpty(itemId)) {
        INTERACTED_ITEM_IDS.add(itemId);
    }
    notifyMutation();
}
```

What that function does: I remember this viewer already acted on the item, then I tell the app to refresh.
What the function is responsible for: I hide claim actions right away, even before the server list updates.
Where it works: I am called by ItemDetailsActivity after a claim and by ReportActivity after a targeted find.
Why I chose to write the code of the function the way its written: I skip empty ids. ConcurrentHashMap lets any thread add.
More: I use TextUtils.isEmpty, INTERACTED_ITEM_IDS, and notifyMutation.

```java
public static boolean wasItemInteracted(@Nullable String itemId) {
    return !TextUtils.isEmpty(itemId) && INTERACTED_ITEM_IDS.contains(itemId);
}
```

What that function does: I return true if this viewer already acted on that item in this process.
What the function is responsible for: I let details keep the "already acted" UI after a local action.
Where it works: I am called by ItemDetailsActivity.
Why I chose to write the code of the function the way its written: I treat empty ids as not interacted.
More: I use TextUtils.isEmpty and a ConcurrentHashMap-backed set.

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
Where it works: I am called by HomeFragment and ItemDetailsActivity.
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
