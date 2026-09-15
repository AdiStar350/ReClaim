```java
private PendingReportStore() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep this class as static methods only.
Where it works: I am not called. Java uses me when someone tries `new PendingReportStore()`.
Why I chose to write the code of the function the way its written: I made the constructor private so enqueue and adopt stay static.
More: I use a private constructor. The class is final.

```java
public static void enqueue(@NonNull Context context,
                           @NonNull Item item,
                           @Nullable Uri imageUri,
                           @Nullable String userId) throws IOException {
    Context app = context.getApplicationContext();
    PendingReportEntity entity = new PendingReportEntity();
    entity.title = item.getTitle();
    entity.description = item.getDescription();
    entity.category = item.getCategory();
    entity.location = item.getLocation();
    entity.type = item.getType();
    entity.verificationQuestion = item.getVerificationQuestion();
    entity.targetLostReportId = item.getTargetLostReportId();
    entity.latitude = item.getLatitude();
    entity.longitude = item.getLongitude();
    entity.remoteImageUrl = item.getImageUrl();
    entity.userId = emptyToNull(userId);
    entity.createdAtEpochMs = System.currentTimeMillis();
    if (imageUri != null && (entity.remoteImageUrl == null || entity.remoteImageUrl.isEmpty())) {
        entity.localImagePath = copyImage(app, imageUri);
    }

    ReclaimDatabase.getInstance(app).pendingReportDao().insert(entity);
    Log.d(TAG, "Queued offline report title=" + entity.title
            + " assigned=" + (entity.userId != null));
    if (entity.userId != null) {
        ReportSyncScheduler.enqueue(app);
    }
}
```

What that function does: I save a report for later send, and I copy the photo into app files if needed.
What the function is responsible for: I am the offline queue write.
Where it works: I am called by ReportActivity when the API is down or the post fails.
Why I chose to write the code of the function the way its written: I only copy the image if there is no remote URL yet. I queue sync only when a user id is known.
More: I use Room insert, copyImage, emptyToNull, and ReportSyncScheduler.enqueue.

```java
public static void adoptUnassignedAndSync(@NonNull Context context) {
    Context app = context.getApplicationContext();
    IO.execute(() -> {
        String userId = TokenManager.getUserId(app);
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Session has no user id; leaving reports unassigned");
            return;
        }
        PendingReportDao dao = ReclaimDatabase.getInstance(app).pendingReportDao();
        List<PendingReportEntity> unassigned = dao.getUnassigned();
        if (!unassigned.isEmpty()) {
            int adopted = dao.assignUnassigned(userId);
            Log.d(TAG, "Adopted " + adopted + " unassigned offline report(s) for user "
                    + userId);
        }
        if (dao.countForUser(userId) > 0) {
            ReportSyncScheduler.enqueueAfterAuth(app);
        }
    });
}
```

What that function does: I give guest drafts to the signed-in user, then I start a send job.
What the function is responsible for: I attach leftover queue rows after login.
Where it works: I am called by AuthSessionListener.onAuthenticated from TokenManager.saveToken.
Why I chose to write the code of the function the way its written: I run on a single IO thread so Room is not on the main thread. REPLACE work is used after auth.
More: I use Executors.newSingleThreadExecutor, TokenManager.getUserId, and ReportSyncScheduler.enqueueAfterAuth.

```java
@Nullable
private static String emptyToNull(@Nullable String userId) {
    if (userId == null || userId.isEmpty()) {
        return null;
    }
    return userId;
}
```

What that function does: I turn an empty user id into null.
What the function is responsible for: I keep guest rows with a real null owner.
Where it works: I am called by enqueue.
Why I chose to write the code of the function the way its written: DAO queries treat null and empty as unassigned, but I store null so the column is clean.
More: I return the same string when it has text.

```java
@Nullable
private static String copyImage(@NonNull Context context, @NonNull Uri imageUri)
        throws IOException {
    File dir = new File(context.getFilesDir(), IMAGE_DIR);
    if (!dir.exists() && !dir.mkdirs()) {
        throw new IOException("Could not create pending image directory");
    }
    File dest = new File(dir, UUID.randomUUID() + ".jpg");
    try (InputStream in = context.getContentResolver().openInputStream(imageUri);
         OutputStream out = new FileOutputStream(dest)) {
        if (in == null) {
            Log.w(TAG, "Could not open image for offline copy: " + imageUri);
            return null;
        }
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
    return dest.getAbsolutePath();
}
```

What that function does: I copy the chosen photo into app storage and return the file path.
What the function is responsible for: I keep a local file so sync can upload later.
Where it works: I am called by enqueue when there is a Uri and no remote URL.
Why I chose to write the code of the function the way its written: I use a UUID name so two drafts do not overwrite. A null stream returns null instead of crashing.
More: I use ContentResolver.openInputStream, FileOutputStream, and UUID.

```java
public static void deleteLocalImage(@Nullable String path) {
    if (path == null || path.isEmpty()) {
        return;
    }
    File file = new File(path);
    if (file.exists() && !file.delete()) {
        Log.w(TAG, "Could not delete synced local image " + path);
    }
}
```

What that function does: I delete a copied photo after the report is sent or dropped.
What the function is responsible for: I clean app files so drafts do not pile up.
Where it works: I am called by ReportSyncWorker.syncOne after success or a hard reject.
Why I chose to write the code of the function the way its written: I ignore a missing path. I only log if delete fails.
More: I use File.delete.
