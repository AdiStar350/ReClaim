# AppStateEvents.java

## Overview
This file is a **singleton event bus** built on `LiveData` (an observable data holder;
see `docs/GLOSSARY.md`). It is a slightly richer sibling of `AppDataRefresh`: besides
telling screens that item or claim data changed, it also remembers which items the
current viewer has already acted on, so details screens can immediately show "already
claimed" without waiting for a server round-trip.

## Concepts used here
- **LiveData / MutableLiveData.** An observable signal; see `docs/GLOSSARY.md`.
- **"New events only" pattern.** Like `AppDataRefresh`, it skips the value present at
  subscribe time to avoid double-fetching.
- **Thread-safe set.** A concurrent set records interacted item ids so it is safe to
  read/write from different threads.
- **Fan-out to `AppDataRefresh`.** A mutation here also ticks `AppDataRefresh`, so both
  signals stay in sync.

## Libraries and imports
- **AndroidX Lifecycle (`LifecycleOwner`, `LiveData`, `MutableLiveData`).** Observe and
  publish the change signal.
- **AndroidX annotations (`NonNull`, `Nullable`).** Document nullability.
- **Android (`android.text.TextUtils`).** `TextUtils.isEmpty(...)` safely checks for
  null/empty item ids.
- **Java standard (`java.util.Collections`, `Set`,
  `java.util.concurrent.ConcurrentHashMap`).** Build a thread-safe set from a concurrent
  map to track interacted items.

## Fields
- `private static final MutableLiveData<Long> SIGNAL` — the change signal (a timestamp
  that ticks on each mutation).
- `private static final Set<String> INTERACTED_ITEM_IDS` — a thread-safe set of item
  ids the current viewer has already acted on, backed by a `ConcurrentHashMap`.

## Functions
Helper (private) first, then public methods.

### `private static long currentTick()`
```java
private static long currentTick() {
    Long value = SIGNAL.getValue();
    return value != null ? value : 0L;
}
```
- **What it does:** returns the signal's current value, or `0` if never set.
- **Where it's called:** by `observeNew`, to capture the baseline.
- **Why it's written this way:** it handles the "never ticked" case cleanly.
- **Step by step:** 1) read the value; 2) return it or `0`.

### `public static LiveData<Long> live()`
```java
public static LiveData<Long> live() {
    return SIGNAL;
}
```
- **What it does:** exposes the change signal as read-only `LiveData`.
- **Where it's called:** available for observers; screens mostly use `observeNew`
  instead.
- **Why it's written this way:** returning `LiveData` prevents outside code from
  publishing ticks.
- **Step by step:** 1) return the shared signal as `LiveData`.

### `public static void notifyMutation()`
```java
public static void notifyMutation() {
    SIGNAL.postValue(System.currentTimeMillis());
    AppDataRefresh.notifyChanged();
}
```
- **What it does:** publishes a change tick and also ticks `AppDataRefresh`.
- **Where it's called:** `RetrofitClient` (after a successful data-changing call) and
  `ReclaimMessagingService` (when a relevant push arrives). Also indirectly via
  `markItemInteracted`.
- **Why it's written this way:** one call updates both event buses so every screen —
  whether it observes `AppStateEvents` or `AppDataRefresh` — learns the data changed.
  `postValue` makes it safe from any thread.
- **Step by step:** 1) post the current time; 2) forward the change to `AppDataRefresh`.

### `public static void markItemInteracted(@Nullable String itemId)`
```java
public static void markItemInteracted(@Nullable String itemId) {
    if (!TextUtils.isEmpty(itemId)) {
        INTERACTED_ITEM_IDS.add(itemId);
    }
    notifyMutation();
}
```
- **What it does:** remembers that the viewer acted on this item, then broadcasts a
  mutation.
- **Where it's called:** `ItemDetailsActivity` (after submitting a claim) and
  `ReportActivity` (after acting on a targeted found report).
- **Why it's written this way:** recording the id locally lets the UI show the item as
  "already handled" instantly, even before the server list refreshes; empty ids are
  ignored to keep the set clean.
- **Step by step:** 1) if the id is non-empty, add it to the set; 2) call
  `notifyMutation` so screens refresh.

### `public static boolean wasItemInteracted(@Nullable String itemId)`
```java
public static boolean wasItemInteracted(@Nullable String itemId) {
    return !TextUtils.isEmpty(itemId) && INTERACTED_ITEM_IDS.contains(itemId);
}
```
- **What it does:** returns `true` if the viewer already acted on the given item.
- **Where it's called:** `ItemDetailsActivity`, to decide whether to disable the claim
  button / show an "already acted" state.
- **Why it's written this way:** a quick local check avoids re-showing an action the
  user already took, even if the server list has not reloaded yet.
- **Step by step:** 1) return `false` for empty ids; 2) otherwise return whether the set
  contains the id.

### `public static void observeNew(LifecycleOwner owner, Runnable onChanged)`
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
- **What it does:** runs `onChanged` only for ticks after this subscribe, tied to the
  lifecycle.
- **Where it's called:** `HomeFragment` and `ItemDetailsActivity` to refresh from the
  server when item/claim data changes.
- **Why it's written this way:** the baseline skips the already-present value so opening
  a screen does not trigger a redundant fetch; the `LifecycleOwner` auto-removes the
  observer.
- **Step by step:**
  1. Capture the current tick as `baseline`.
  2. Observe for the given lifecycle owner.
  3. On each newer tick, run `onChanged`.
