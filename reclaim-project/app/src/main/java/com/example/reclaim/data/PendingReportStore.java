package com.example.reclaim.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.reclaim.model.Item;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.sync.ReportSyncScheduler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Saves unsynced reports and copies photos into app storage.
 */
public final class PendingReportStore {

    private static final String TAG = "PendingReportStore";
    private static final String IMAGE_DIR = "pending_reports";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private PendingReportStore() {
    }

    /**
     * Saves a report for later send.
     * {@code userId} may be {@code null}; it is claimed on the next login.
     */
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

    /**
     * Gives unowned queued reports to the signed-in user
     * and queues a send. Fine from the main thread.
     */
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

    @Nullable
    private static String emptyToNull(@Nullable String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        return userId;
    }

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

    public static void deleteLocalImage(@Nullable String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        File file = new File(path);
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Could not delete synced local image " + path);
        }
    }
}
