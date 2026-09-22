# ClaimDecisionEvents.java

## Overview
This file is a tiny **app-wide LiveData signal** that fires when a `CLAIM_DECISION`
push arrives (approve/reject of a claim). Home observes it so the claimant-facing UI
can refresh without waiting for the next `onResume`. Same pattern as
`IncomingClaimEvents`, but for decision pushes.

## Concepts used here
- **LiveData / MutableLiveData.** Observable holders; UI updates when the value
  changes; see `docs/GLOSSARY.md`.
- **Firebase / FCM (indirect).** `ReclaimMessagingService` receives the push and
  calls `notifyReceived`; see Firebase in `docs/GLOSSARY.md`.
- **Static utility / event bus pattern.** One shared signal; private constructor.

## Libraries and imports
- **AndroidX Lifecycle (`LiveData`, `MutableLiveData`).** Hold and expose the signal.

## Fields
- `private static final MutableLiveData<Long> SIGNAL` — shared event stream posted
  with a timestamp when a decision push arrives.

## Functions

### `private ClaimDecisionEvents()`
```java
private ClaimDecisionEvents() {
}
```
- **What it does:** empty private constructor blocking `new ClaimDecisionEvents()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** static-only event holder.
- **Step by step:** 1) private access; 2) empty body; 3) no instances from outside.

### `public static LiveData<Long> live()`
```java
public static LiveData<Long> live() {
    return SIGNAL;
}
```
- **What it does:** returns the read-only LiveData observers should watch.
- **Where it's called:** `HomeFragment` observes it to refresh claim-decision UI
  while the home screen is open.
- **Why it's written this way:** exposing `LiveData` only keeps mutation inside
  `notifyReceived`.
- **Step by step:** 1) return the shared `SIGNAL` as `LiveData`.

### `public static void notifyReceived()`
```java
public static void notifyReceived() {
    SIGNAL.postValue(System.currentTimeMillis());
}
```
- **What it does:** posts a new timestamp so observers know a `CLAIM_DECISION` push
  arrived.
- **Where it's called:** `ReclaimMessagingService.onMessageReceived` when the push
  type is `CLAIM_DECISION`.
- **Why it's written this way:** `postValue` is safe from FCM's background thread; a
  fresh millis value always notifies observers.
- **Step by step:** 1) `postValue` the current time millis onto `SIGNAL`.
