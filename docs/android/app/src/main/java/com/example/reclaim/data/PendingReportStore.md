# PendingReportStore.java

## Overview
This file is a **singleton helper** (all-static utility) that manages offline reports.
When a report cannot be sent because the device or backend is offline, this class saves
it into the Room database and copies its photo into the app's private storage, so
nothing is lost. Later it hands guest reports to the signed-in user and asks the sync
scheduler to send them. See Room and WorkManager in `docs/GLOSSARY.md`.

## Concepts used here
- **Room database.** Used to store the queued report rows; see `docs/GLOSSARY.md`.
- **Background thread (ExecutorService).** Database and file work must not run on the
  main (UI) thread, so a single background thread is used.
- **File I/O.** The chosen photo is copied from a content `Uri` into a private app file
  so it still exists when the report is sent later.
- **WorkManager (indirect).** After queuing, it calls `ReportSyncScheduler` to run the
  sync job; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Android (`android.content.Context`, `android.net.Uri`, `android.util.Log`).**
  `Context` reaches app storage and the content resolver; `Uri` points at the chosen
  image; `Log` records progress.
- **AndroidX annotations (`androidx.annotation.NonNull`, `Nullable`).** Document
  nullability.
- **Project classes (`Item`, `TokenManager`, `ReportSyncScheduler`,
  `ReclaimDatabase`/`PendingReportDao`/`PendingReportEntity`).** Convert data, read the
  user id, schedule the sync, and reach the database.
- **Java standard (`java.io.*`, `java.util.List`, `UUID`,
  `ExecutorService`/`Executors`).** File copy streams, unique file names, and the
  background executor.

## Fields
- `private static final String TAG` — the log tag `"PendingReportStore"`.
- `private static final String IMAGE_DIR = "pending_reports"` — the private folder name
  where offline photos are copied.
- `private static final ExecutorService IO` — a single-thread executor used to run
  database/file work off the UI thread.

## Functions
Helper (private) methods first, then the public entry points.

### `private static String emptyToNull(@Nullable String userId)`
```java
@Nullable
private static String emptyToNull(@Nullable String userId) {
    if (userId == null || userId.isEmpty()) {
        return null;
    }
    return userId;
}
```
- **What it does:** turns an empty or null user id into `null`.
- **Where it's called:** by `enqueue`, when setting the entity's `userId`.
- **Why it's written this way:** it normalizes "no owner" to a single value (`null`), so
  the "unassigned" queries have one consistent case to match.
- **Step by step:** 1) if null or empty, return `null`; 2) otherwise return the id.

### `private static String copyImage(@NonNull Context context, @NonNull Uri imageUri)`
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
- **What it does:** copies the picked image into a new file in the app's private
  storage and returns that file path.
- **Where it's called:** by `enqueue`, when a report has a local image but no uploaded
  URL yet.
- **Why it's written this way:** the original `Uri` may become unreadable later (the
  user could remove permission or the picker cache could clear), so a private copy makes
  sure the photo survives until upload. It uses a random file name to avoid clashes and
  try-with-resources so the streams always close.
- **Step by step:**
  1. Make the `pending_reports` folder if it does not exist (throw if that fails).
  2. Build a destination file with a random name.
  3. Open the source stream and the destination stream together.
  4. If the source cannot be opened, log and return `null`.
  5. Copy in 8 KB chunks until the source is exhausted.
  6. Return the saved file's absolute path.

### `public static void enqueue(Context context, Item item, Uri imageUri, String userId)`
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
- **What it does:** saves a report for later sending, copying its image locally if it
  has not been uploaded, and schedules a sync if the report already has an owner.
- **Where it's called:** `ReportActivity`, when a report is created while the backend is
  unreachable (and the report is a new one, not an edit).
- **Why it's written this way:** it turns an `Item` into a Room entity, records the
  creation time so ordering works, and only copies the photo when there is no uploaded
  URL yet (saving space). If there is no `userId`, it leaves the report unassigned and
  does not schedule work — the report will be adopted and synced after the next login.
- **Step by step:**
  1. Get the application context.
  2. Build a `PendingReportEntity` from the `Item`'s fields.
  3. Normalize the `userId` and stamp the current time.
  4. If there is an image and no uploaded URL, copy the image locally and store the
     path.
  5. Insert the row into the Room database.
  6. If the report has an owner, call `ReportSyncScheduler.enqueue` to try sending it.

### `public static void adoptUnassignedAndSync(@NonNull Context context)`
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
- **What it does:** on a background thread, gives all guest (unassigned) reports to the
  signed-in user and, if that user now has queued reports, schedules a sync.
- **Where it's called:** `AuthSessionListener.onAuthenticated`, which runs right after
  `TokenManager.saveToken` — i.e. just after login or sign-up.
- **Why it's written this way:** database work must be off the UI thread, so it runs
  inside `IO.execute(...)`; the comment notes it is safe to call from the main thread
  for that reason. It first checks there is a user id, then only schedules work if there
  is actually something to send.
- **Step by step:**
  1. Get the application context and run the rest on the background executor.
  2. Read the current user id; if there is none, log and stop.
  3. Get the DAO and fetch unassigned reports.
  4. If any exist, assign them all to this user.
  5. If the user now has one or more queued reports, call `enqueueAfterAuth` to run the
     sync (replacing any older work).

### `public static void deleteLocalImage(@Nullable String path)`
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
- **What it does:** deletes a locally copied report image after it is no longer needed.
- **Where it's called:** `ReportSyncWorker.syncOne`, after a queued report is
  successfully sent or permanently dropped.
- **Why it's written this way:** cleaning up the copied photo keeps app storage from
  filling with images for reports that are already uploaded; it fails quietly (just a
  log) if the file cannot be removed.
- **Step by step:** 1) ignore null/empty paths; 2) build the file; 3) if it exists and
  cannot be deleted, log a warning.
