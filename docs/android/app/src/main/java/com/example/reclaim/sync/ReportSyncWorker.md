```java
public ReportSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
}
```

What that function does: I pass context and WorkManager params to Worker.
What the function is responsible for: I am the constructor WorkManager uses to create me.
Where it works: WorkManager builds me from ReportSyncScheduler's OneTimeWorkRequest.
Why I chose to write the code of the function the way its written: Worker requires this constructor signature.
More: I call super(context, params).

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

What that function does: I claim guest rows, then I send this user's queued reports.
What the function is responsible for: I am the WorkManager job body.
Where it works: WorkManager runs me when the network is up, from ReportSyncScheduler.
Why I chose to write the code of the function the way its written: No session returns success so WorkManager does not retry forever while logged out. A failed send returns retry.
More: I use TokenManager, ReclaimDatabase, and Result.retry.

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

What that function does: I upload a local photo if needed, POST the item, then drop the row on success or a hard 4xx.
What the function is responsible for: I send one queued report.
Where it works: I am called in a loop from doWork.
Why I chose to write the code of the function the way its written: 401, 408, and 429 should retry. Other 4xx are bad fields, so I delete. execute() is blocking, which a Worker allows.
More: I use ImageUploadService.uploadImage, RetrofitClient.createItem, PendingReportStore.deleteLocalImage, and AppNotifications.showReportSynced.
