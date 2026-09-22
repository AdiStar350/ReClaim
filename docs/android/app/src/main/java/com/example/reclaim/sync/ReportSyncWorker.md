# ReportSyncWorker.java

## Overview
This file is a **WorkManager Worker** that sends lost/found reports saved while
offline. For the signed-in user it adopts any guest rows, uploads a local photo if
needed, posts each item to the API with Retrofit, then deletes the local row (and
photo) on success. Hard client errors drop the row so the queue does not retry
forever; other failures ask WorkManager to retry.

## Concepts used here
- **WorkManager / Worker.** Background job that runs when constraints are met; see
  `docs/GLOSSARY.md`.
- **JWT / auth header.** `TokenManager` supplies `"Bearer <token>"` for API calls;
  see `docs/GLOSSARY.md`.
- **Retrofit `Call` / `execute`.** Synchronous HTTP on the worker thread (not
  `enqueue`); see `docs/GLOSSARY.md`.
- **HTTP status codes.** 2xx success; most 4xx (except 401/408/429) mean "drop";
  other codes retry; see `docs/GLOSSARY.md`.
- **Firebase Storage (indirect).** `ImageUploadService` uploads the photo before the
  item is created; see Firebase in `docs/GLOSSARY.md`.
- **Toast / notification.** After a successful sync, a local notification is shown via
  `AppNotifications`.

## Libraries and imports
- **Android (`Context`, `Uri`, `Log`).** App context, file URI for uploads, logging.
- **AndroidX Work (`Worker`, `WorkerParameters`, `Result`).** Worker base class and
  success/retry results.
- **Java (`File`, `IOException`, `List`).** Local image file and errors.
- **Retrofit (`Response`).** Inspect HTTP status after `execute()`.
- **Project classes.** `PendingReportDao` / `PendingReportEntity` / `ReclaimDatabase` /
  `PendingReportStore` — local queue; `Item` — API body; `RetrofitClient` /
  `TokenManager` — API + auth; `ImageUploadService` — photo upload;
  `AppNotifications` — "report synced" notice.

## Fields
- `private static final String TAG = "ReportSyncWorker"` — log tag for sync messages.

## Functions

### `private void syncOne(...)`
```java
private void syncOne(@NonNull Context context,
                     @NonNull String authHeader,
                     @NonNull PendingReportDao dao,
                     @NonNull PendingReportEntity entity) throws IOException {
    Item item = entity.toItem();

    // Send the saved photo first, if there is one.
    if ((item.getImageUrl() == null || item.getImageUrl().isEmpty())
            && entity.localImagePath != null) {
        File imageFile = new File(entity.localImagePath);
        if (imageFile.exists() && entity.userId != null && !entity.userId.isEmpty()) {
            String url = ImageUploadService.uploadImage(
                    context, Uri.fromFile(imageFile), entity.userId);
            item.setImageUrl(url);
        }
    }

    Response<Item> response = RetrofitClient.getApiService()
            .createItem(authHeader, item)
            .execute();

    if (response.isSuccessful()) {
        PendingReportStore.deleteLocalImage(entity.localImagePath);
        dao.delete(entity);
        Log.d(TAG, "Synced pending report id=" + entity.id + " title=" + entity.title);
        AppNotifications.showReportSynced(context, entity.title);
        return;
    }

    int code = response.code();
    if (code >= 400 && code < 500 && code != 401 && code != 408 && code != 429) {
        // Rejected for good (bad fields, etc.). Drop it
        // so the queue does not retry forever.
        Log.w(TAG, "Backend rejected pending report id=" + entity.id
                + " http=" + code + "; removing from queue");
        PendingReportStore.deleteLocalImage(entity.localImagePath);
        dao.delete(entity);
        return;
    }

    throw new IOException("Backend returned HTTP " + code + " for pending report");
}
```
- **What it does:** uploads one pending report (photo if needed, then `createItem`),
  then deletes the local row on success or on a permanent 4xx reject; otherwise throws
  so the caller can retry.
- **Where it's called:** only from `doWork` inside the pending loop.
- **Why it's written this way:** photo-first avoids creating an item without an image
  URL; dropping most 4xx stops poison messages; 401/408/429 and 5xx stay retryable.
- **Step by step:**
  1. Build an `Item` from the entity.
  2. If there is no remote URL but a local file exists, upload it and set `imageUrl`.
  3. `execute()` `createItem` with the auth header.
  4. On success: delete local image + DB row, notify the user, return.
  5. On hard 4xx (not 401/408/429): delete local data and return without throwing.
  6. Otherwise throw `IOException` with the status code.

### `public ReportSyncWorker(@NonNull Context context, @NonNull WorkerParameters params)`
```java
public ReportSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
}
```
- **What it does:** constructs the worker and passes context/params to `Worker`.
- **Where it's called:** WorkManager reflects this constructor when running the
  enqueued `OneTimeWorkRequest` from `ReportSyncScheduler`.
- **Why it's written this way:** required Worker constructor shape for WorkManager.
- **Step by step:** 1) call `super(context, params)`.

### `public Result doWork()`
```java
@NonNull
@Override
public Result doWork() {
    Context context = getApplicationContext();
    String userId = TokenManager.getUserId(context);
    String authHeader = TokenManager.getAuthHeader(context);
    if (authHeader == null || userId == null || userId.isEmpty()) {
        // Not signed in; keep reports queued.
        Log.w(TAG, "No auth session; keeping pending reports queued");
        return Result.success();
    }

    PendingReportDao dao = ReclaimDatabase.getInstance(context).pendingReportDao();
    int adopted = dao.assignUnassigned(userId);
    if (adopted > 0) {
        Log.d(TAG, "Adopted " + adopted + " unassigned report(s) before sync");
    }

    List<PendingReportEntity> pending = dao.getByUserId(userId);
    if (pending.isEmpty()) {
        return Result.success();
    }
    Log.d(TAG, "Syncing " + pending.size() + " pending report(s) for user " + userId);

    boolean allSynced = true;
    for (PendingReportEntity entity : pending) {
        try {
            syncOne(context, authHeader, dao, entity);
        } catch (Exception e) {
            allSynced = false;
            Log.e(TAG, "Failed to sync report id=" + entity.id
                    + " title=" + entity.title, e);
        }
    }

    return allSynced ? Result.success() : Result.retry();
}
```
- **What it does:** if signed in, adopts guest rows, syncs each pending report for the
  user, and returns success or retry depending on whether all succeeded.
- **Where it's called:** WorkManager when the unique sync work runs (after
  `ReportSyncScheduler.enqueue` / `enqueueAfterAuth`).
- **Why it's written this way:** returning `success` when logged out keeps the rows
  queued without endless retries; adopting first ties guest drafts to this user;
  per-item try/catch lets one failure retry later without blocking the whole batch
  forever in one crash.
- **Step by step:**
  1. Read `userId` and auth header; if missing, log and return `Result.success()`.
  2. Open the DAO; assign unassigned rows to this user.
  3. Load pending rows for the user; return success if empty.
  4. For each entity, call `syncOne`; on exception mark `allSynced = false`.
  5. Return `Result.success()` if all synced, else `Result.retry()`.
