# AppDataRefresh.java

## Overview
This file is a **singleton event bus** built on `LiveData`. `LiveData` is an Android
data holder you can *observe*; when its value changes, observers are notified (see
`docs/GLOSSARY.md`). `AppDataRefresh` is an app-wide "something on the server changed,
you may want to reload" signal. Screens observe it and refetch their data when it ticks.

## Concepts used here
- **LiveData / MutableLiveData.** A value screens can observe; `MutableLiveData` is the
  writable version this class updates; see `docs/GLOSSARY.md`.
- **Lifecycle-aware observing.** Observing with a `LifecycleOwner` means the observer is
  removed automatically when the screen is destroyed, avoiding leaks.
- **"New events only" pattern.** A baseline value is captured at subscribe time so the
  current value is skipped, preventing a double-fetch when a screen opens.
- **Timestamp as a signal.** The value is just "now" in milliseconds — the number does
  not matter, only that it changed.

## Libraries and imports
- **AndroidX Lifecycle (`androidx.lifecycle.LifecycleOwner`, `LiveData`,
  `MutableLiveData`).** `MutableLiveData.postValue(...)` publishes a tick from any
  thread; `observe(owner, ...)` subscribes tied to a lifecycle.
- **AndroidX annotations (`androidx.annotation.NonNull`).** Documents non-null
  parameters.

## Fields
- `private static final MutableLiveData<Long> SIGNAL` — the shared signal; its `Long`
  value is a timestamp that changes each time data is marked stale.

## Functions
Helper (private) first, then public methods.

### `private static long currentTick()`
```java
private static long currentTick() {
    Long value = SIGNAL.getValue();
    return value != null ? value : 0L;
}
```
- **What it does:** returns the signal's current value, or `0` if it has never ticked.
- **Where it's called:** by `observeNew`, to record the baseline before subscribing.
- **Why it's written this way:** it safely handles the "never set yet" case so the
  baseline math always works.
- **Step by step:** 1) read the current value; 2) return it, or `0` if null.

### `public static LiveData<Long> live()`
```java
public static LiveData<Long> live() {
    return SIGNAL;
}
```
- **What it does:** exposes the signal as read-only `LiveData` for observers.
- **Where it's called:** `ContactInfoDialog` observes it directly to refresh while open.
- **Why it's written this way:** returning `LiveData` (not `MutableLiveData`) lets
  others observe but not publish, so only this class controls when it ticks.
- **Step by step:** 1) return the shared signal typed as `LiveData`.

### `public static void notifyChanged()`
```java
public static void notifyChanged() {
    SIGNAL.postValue(System.currentTimeMillis());
}
```
- **What it does:** publishes a new tick (the current time), telling observers data may
  be stale.
- **Where it's called:** `AppStateEvents.notifyMutation`, which fires after a successful
  data-changing network call.
- **Why it's written this way:** it uses `postValue` so it is safe to call from any
  thread; the timestamp guarantees the value is different each time.
- **Step by step:** 1) post the current time as the new value.

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
- **What it does:** runs `onChanged` only for ticks that happen *after* this call, tied
  to the given lifecycle.
- **Where it's called:** `ProfileSettingsFragment`, `ReportHistoryActivity`,
  `MyItemsFragment`, and `SearchFragment` to reload when data
  changes.
- **Why it's written this way:** capturing a baseline and only reacting to greater ticks
  skips the value that already existed when the screen opened, so a screen does not fetch
  twice (once in its own load, once from an old tick). Observing with a `LifecycleOwner`
  auto-removes the observer when the screen goes away.
- **Step by step:**
  1. Record the current tick as `baseline`.
  2. Observe the signal for this lifecycle owner.
  3. On each tick, if it is newer than the baseline, run `onChanged`.
