package com.example.reclaim.sync;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.reclaim.data.PendingReportDao;
import com.example.reclaim.data.PendingReportEntity;
import com.example.reclaim.data.PendingReportStore;
import com.example.reclaim.data.ReclaimDatabase;
import com.example.reclaim.model.Item;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.notifications.AppNotifications;
import com.example.reclaim.storage.ImageUploadService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * Uploads reports that were saved locally while the device was offline.
 * <p>
 * For each queued report: upload the cached photo to Firebase Storage
 * (if one exists), POST the item to the backend, and delete the local
 * row + image on success. Runs under a WorkManager network constraint
 * and retries with backoff when the connection drops mid-sync.
 * </p>
 */
public class ReportSyncWorker extends Worker {

    private static final String TAG = "ReportSyncWorker";

    public ReportSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String authHeader = TokenManager.getAuthHeader(context);
        if (authHeader == null) {
            // Not logged in; keep reports queued until the user signs in again.
            Log.w(TAG, "No auth token; keeping pending reports queued");
            return Result.success();
        }

        PendingReportDao dao = ReclaimDatabase.getInstance(context).pendingReportDao();
        List<PendingReportEntity> pending = dao.getAll();
        if (pending.isEmpty()) {
            return Result.success();
        }
        Log.d(TAG, "Syncing " + pending.size() + " pending report(s)");

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

    private void syncOne(@NonNull Context context,
                         @NonNull String authHeader,
                         @NonNull PendingReportDao dao,
                         @NonNull PendingReportEntity entity) throws IOException {
        Item item = entity.toItem();

        // Upload the cached photo first, if the report has one.
        if ((item.getImageUrl() == null || item.getImageUrl().isEmpty())
                && entity.localImagePath != null) {
            File imageFile = new File(entity.localImagePath);
            if (imageFile.exists() && entity.userId != null) {
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
            // Permanently rejected (validation etc.) — drop it so the queue
            // doesn't retry forever.
            Log.w(TAG, "Backend rejected pending report id=" + entity.id
                    + " http=" + code + "; removing from queue");
            PendingReportStore.deleteLocalImage(entity.localImagePath);
            dao.delete(entity);
            return;
        }

        throw new IOException("Backend returned HTTP " + code + " for pending report");
    }
}
