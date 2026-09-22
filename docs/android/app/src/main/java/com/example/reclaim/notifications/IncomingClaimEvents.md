# IncomingClaimEvents.java

## Overview
This file is a tiny **app-wide LiveData signal** that fires when a `CLAIM_PENDING`
push arrives. Home observes it so the pending-claim card can refresh without waiting
for the fragment's next `onResume`. The value is just a timestamp; observers care that
something changed, not what the number is.

## Concepts used here
- **LiveData / MutableLiveData.** Observable holders; UI updates when the value
  changes; see `docs/GLOSSARY.md`.
- **Firebase / FCM (indirect).** The push is received in `ReclaimMessagingService`,
  which calls `notifyReceived`; see Firebase in `docs/GLOSSARY.md`.
- **Static utility / event bus pattern.** One shared signal for the whole process;
  private constructor.

## Libraries and imports
- **AndroidX Lifecycle (`LiveData`, `MutableLiveData`).** Hold and expose the signal.
- No other project imports; callers only depend on this class.

## Fields
- `private static final MutableLiveData<Long> SIGNAL` — the shared event stream;
  posted with `System.currentTimeMillis()` when a claim push arrives.

## Functions

### `private IncomingClaimEvents()`
```java
private IncomingClaimEvents() {
}
```
- **What it does:** empty private constructor blocking `new IncomingClaimEvents()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** static-only event holder; no instances needed.
- **Step by step:** 1) private access; 2) empty body; 3) no instances from outside.

### `public static LiveData<Long> live()`
```java
public static LiveData<Long> live() {
    return SIGNAL;
}
```
- **What it does:** returns the read-only LiveData observers should watch.
- **Where it's called:** `HomeFragment` observes it with the view lifecycle owner to
  refresh pending-claim UI when a push arrives while the screen is open.
- **Why it's written this way:** returning `LiveData` (not `MutableLiveData`) stops
  screens from posting into the bus themselves.
- **Step by step:** 1) return the shared `SIGNAL` as `LiveData`.

### `public static void notifyReceived()`
```java
public static void notifyReceived() {
    SIGNAL.postValue(System.currentTimeMillis());
}
```
- **What it does:** posts a new timestamp so observers know a `CLAIM_PENDING` push
  arrived. Safe from any thread.
- **Where it's called:** `ReclaimMessagingService.onMessageReceived` when the push
  type is `CLAIM_PENDING`.
- **Why it's written this way:** `postValue` works off the main thread (FCM callbacks
  may not be on UI); a timestamp always differs so LiveData delivers even if the
  previous value looked "the same kind" of event.
- **Step by step:** 1) `postValue` the current time millis onto `SIGNAL`.
