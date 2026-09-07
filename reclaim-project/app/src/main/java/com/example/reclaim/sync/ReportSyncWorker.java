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
 * Sends reports that were saved while offline.
 * Guest rows are claimed by the current user first.
 * Drops the local row after a successful post.
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
}
