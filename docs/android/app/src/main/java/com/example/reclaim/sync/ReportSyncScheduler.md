```java
private ReportSyncScheduler() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep enqueue as static methods.
Where it works: I am not called. Java uses me when someone tries `new ReportSyncScheduler()`.
Why I chose to write the code of the function the way its written: I made the constructor private so WorkManager is the only runner.
More: I use a private constructor. The class is final.

```java
public static void enqueue(@NonNull Context context) {
    enqueue(context, ExistingWorkPolicy.KEEP);
}
```

What that function does: I queue unique sync work if none is already waiting.
What the function is responsible for: I am the normal enqueue that does not replace running work.
Where it works: I am called by ReclaimApp, PendingReportStore.enqueue, and NetworkMonitor.onAvailable.
Why I chose to write the code of the function the way its written: KEEP avoids restarting a send that is already in flight.
More: I forward to the private enqueue with ExistingWorkPolicy.KEEP.

```java
public static void enqueueAfterAuth(@NonNull Context context) {
    enqueue(context, ExistingWorkPolicy.REPLACE);
}
```

What that function does: I replace older sync work so a new login can retry drafts that waited.
What the function is responsible for: I am the post-login enqueue.
Where it works: I am called by PendingReportStore.adoptUnassignedAndSync.
Why I chose to write the code of the function the way its written: REPLACE is needed because KEEP would leave work that ran while unsigned and returned early.
More: I forward to the private enqueue with ExistingWorkPolicy.REPLACE.

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

What that function does: I schedule one unique ReportSyncWorker that waits for a network.
What the function is responsible for: I am the WorkManager write.
Where it works: I am called by the two public enqueue methods.
Why I chose to write the code of the function the way its written: Unique work name reclaim_pending_report_sync stops duplicate queues. Exponential backoff waits 30s then longer.
More: I use WorkManager, OneTimeWorkRequest, NetworkType.CONNECTED, and BackoffPolicy.EXPONENTIAL.
