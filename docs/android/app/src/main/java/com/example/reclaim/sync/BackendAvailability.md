# BackendAvailability.java

## Overview
This file is a **singleton helper** that remembers whether the backend answered the last
time the app called it. This is separate from device connectivity: the phone can have
internet while the server itself is down or unreachable. The networking interceptor in
`RetrofitClient` updates this flag on every call; the rest of the app reads it. See
HTTP status codes in `docs/GLOSSARY.md`.

## Concepts used here
- **Reachability flag.** A single boolean meaning "the API last answered."
- **`volatile`.** Marks the flag so every thread sees the latest written value
  immediately (the flag is written from network threads and read from the UI).
- **Gateway/outage status codes.** Certain HTTP codes mean the request finished but the
  server behind the gateway did not really answer.

## Libraries and imports
This class uses only core Java — no imports beyond the language. It calls into
`OfflineProtocol.resetOutageAck()` (same package) when the backend becomes reachable
again.

## Fields
- `private static volatile boolean reachable = true` — whether the API last answered.
  It starts `true` (optimistic) and is `volatile` for cross-thread visibility.

## Functions

### `public static boolean isReachable()`
```java
public static boolean isReachable() {
    return reachable;
}
```
- **What it does:** returns whether the backend was last seen as reachable.
- **Where it's called:** `NetworkMonitor.isBackendAvailable` and
  `OfflineProtocol.showIfNeeded`.
- **Why it's written this way:** a simple getter over the `volatile` field gives other
  classes a consistent view of backend state.
- **Step by step:** 1) return the current flag.

### `public static void markReachable()`
```java
public static void markReachable() {
    reachable = true;
    OfflineProtocol.resetOutageAck();
}
```
- **What it does:** records that the backend answered, and clears the "user already
  dismissed the outage dialog" flag.
- **Where it's called:** `RetrofitClient.interceptBackendAvailability` after a
  successful (non-outage) response.
- **Why it's written this way:** when the server comes back, the previous outage is over,
  so the acknowledgement is reset — that way a *future* outage will show the dialog again
  instead of being silently suppressed.
- **Step by step:** 1) set `reachable = true`; 2) call
  `OfflineProtocol.resetOutageAck()`.

### `public static void markUnreachable()`
```java
public static void markUnreachable() {
    reachable = false;
}
```
- **What it does:** records that the backend did not answer.
- **Where it's called:** `RetrofitClient.interceptBackendAvailability` on an outage
  status code or a network exception.
- **Why it's written this way:** a plain flip of the flag; the interceptor separately
  triggers the offline dialog.
- **Step by step:** 1) set `reachable = false`.

### `public static boolean isUnreachableStatus(int code)`
```java
public static boolean isUnreachableStatus(int code) {
    return code == 502 || code == 503 || code == 504
            || code == 521 || code == 522 || code == 523;
}
```
- **What it does:** returns `true` for HTTP status codes that mean the API did not
  really answer, even though an HTTP response came back.
- **Where it's called:** `RetrofitClient.interceptBackendAvailability`, to decide whether
  a response counts as an outage.
- **Why it's written this way:** codes 502/503/504 are standard "bad gateway / service
  unavailable / gateway timeout"; 521/522/523 are tunnel/proxy errors (the ngrok tunnel
  or Cloudflare-style front) meaning the real server was not reached. Treating these as
  unreachable lets the app switch to offline handling.
- **Step by step:** 1) return whether the code is one of the listed outage codes.
