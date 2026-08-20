package com.example.reclaim.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.reclaim.model.Item;
import com.example.reclaim.sync.ReportSyncScheduler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Persists unsynced item reports and copies photos into app-private storage.
 */
public final class PendingReportStore {

    private static final String TAG = "PendingReportStore";
    private static final String IMAGE_DIR = "pending_reports";

    private PendingReportStore() {
    }

    /**
     * Saves a new report for later upload and schedules background sync.
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
        entity.userId = userId;
        entity.createdAtEpochMs = System.currentTimeMillis();
        if (imageUri != null && (entity.remoteImageUrl == null || entity.remoteImageUrl.isEmpty())) {
            entity.localImagePath = copyImage(app, imageUri);
        }

        ReclaimDatabase.getInstance(app).pendingReportDao().insert(entity);
        Log.d(TAG, "Queued offline report title=" + entity.title);
        ReportSyncScheduler.enqueue(app);
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
