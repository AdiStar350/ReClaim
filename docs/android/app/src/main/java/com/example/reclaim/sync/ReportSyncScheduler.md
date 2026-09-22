# ReportSyncScheduler.java

## Overview
This file is a **static helper** that queues a unique WorkManager job to upload
offline reports. The work only runs when the device has a network connection. Callers
either keep an existing job (`KEEP`) or replace it after login (`REPLACE`) so uploads
that waited while unsigned can retry.

## Concepts used here
- **WorkManager.** Android Jetpack API that runs background jobs reliably; see
  `docs/GLOSSARY.md`.
- **Constraints / NetworkType.** The job waits until the phone is online.
- **Unique work.** One named job (`UNIQUE_WORK_NAME`) so duplicate enqueues do not
  pile up.
- **Backoff.** If the worker asks to retry, WorkManager waits longer each time
  (exponential, starting at 30 seconds).
- **`Context`.** Needed to get the app-wide `WorkManager` instance; see
  `docs/GLOSSARY.md`.

## Libraries and imports
- **Android (`android.content.Context`).** Application context for WorkManager.
- **AndroidX Work (`Constraints`, `OneTimeWorkRequest`, `WorkManager`,
  `ExistingWorkPolicy`, `BackoffPolicy`, `NetworkType`).** Build and enqueue the
  sync job.
- **Java (`TimeUnit`).** Units for the backoff delay.
- **Project class.** `ReportSyncWorker` — the worker class that actually posts the
  pending reports.

## Fields
- `public static final String UNIQUE_WORK_NAME = "reclaim_pending_report_sync"` — the
  stable name WorkManager uses so only one sync job of this kind is tracked.

## Functions

### `private ReportSyncScheduler()`
```java
private ReportSyncScheduler() {
}
```
- **What it does:** empty private constructor blocking `new ReportSyncScheduler()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** the class is a static-only helper.
- **Step by step:** 1) private access; 2) empty body; 3) no instances from outside.

### `private static void enqueue(@NonNull Context context, @NonNull ExistingWorkPolicy policy)`
```java
private static void enqueue(@NonNull Context context, @NonNull ExistingWorkPolicy policy) {
    Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReportSyncWorker.class)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build();

    WorkManager.getInstance(context.getApplicationContext())
            .enqueueUniqueWork(UNIQUE_WORK_NAME, policy, request);
}
```
- **What it does:** builds a one-time `ReportSyncWorker` request (online-only,
  exponential backoff) and enqueues it under the unique name with the given policy.
- **Where it's called:** by the two public `enqueue` / `enqueueAfterAuth` overloads.
- **Why it's written this way:** shared builder avoids duplicating constraints; the
  policy argument is the only difference between "keep existing" and "replace."
- **Step by step:**
  1. Require `NetworkType.CONNECTED`.
  2. Build a `OneTimeWorkRequest` for `ReportSyncWorker` with that constraint and
     exponential backoff from 30 seconds.
  3. Ask `WorkManager` to enqueue unique work with `UNIQUE_WORK_NAME` and `policy`.

### `public static void enqueue(@NonNull Context context)`
```java
public static void enqueue(@NonNull Context context) {
    enqueue(context, ExistingWorkPolicy.KEEP);
}
```
- **What it does:** schedules sync work if none is already pending; does not replace
  an existing job.
- **Where it's called:** `ReclaimApp.onCreate`, `NetworkMonitor` when the network
  comes back, and `PendingReportStore` after saving a pending report.
- **Why it's written this way:** `KEEP` avoids restarting a job that is already waiting
  or running when many screens enqueue at once.
- **Step by step:** 1) call the private `enqueue` with `ExistingWorkPolicy.KEEP`.

### `public static void enqueueAfterAuth(@NonNull Context context)`
```java
public static void enqueueAfterAuth(@NonNull Context context) {
    enqueue(context, ExistingWorkPolicy.REPLACE);
}
```
- **What it does:** replaces any older sync work so a new signed-in session can retry
  uploads that waited while the user was unsigned.
- **Where it's called:** `PendingReportStore.adoptUnassignedAndSync` after guest rows
  are claimed by the logged-in user.
- **Why it's written this way:** `REPLACE` forces a fresh attempt with the new auth
  session instead of relying on a job that may have exited early while logged out.
- **Step by step:** 1) call the private `enqueue` with `ExistingWorkPolicy.REPLACE`.
