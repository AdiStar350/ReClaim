package com.example.reclaim.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.reclaim.R;
import com.example.reclaim.ui.dashboard.DashboardActivity;
import com.example.reclaim.ui.details.ItemDetailsActivity;
import com.example.reclaim.ui.myitems.MyItemsActivity;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Channels and builders for matches, claims, and sync notices.
 */
public final class AppNotifications {

    private static final String TAG = "AppNotifications";

    public static final String CHANNEL_MATCHES = "matches";
    public static final String CHANNEL_CLAIMS = "claims";
    public static final String CHANNEL_SYNC = "sync";

    private static final AtomicInteger NOTIFICATION_ID =
            new AtomicInteger((int) (System.currentTimeMillis() % 10_000));

    private AppNotifications() {
    }

    /** Makes the channels. Fine to call more than once. */
    public static void createChannels(@NonNull Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel matches = new NotificationChannel(
                CHANNEL_MATCHES,
                context.getString(R.string.notif_channel_matches),
                NotificationManager.IMPORTANCE_HIGH);
        matches.setDescription(context.getString(R.string.notif_channel_matches_desc));

        NotificationChannel claims = new NotificationChannel(
                CHANNEL_CLAIMS,
                context.getString(R.string.notif_channel_claims),
                NotificationManager.IMPORTANCE_HIGH);
        claims.setDescription(context.getString(R.string.notif_channel_claims_desc));

        NotificationChannel sync = new NotificationChannel(
                CHANNEL_SYNC,
                context.getString(R.string.notif_channel_sync),
                NotificationManager.IMPORTANCE_DEFAULT);
        sync.setDescription(context.getString(R.string.notif_channel_sync_desc));

        manager.createNotificationChannel(matches);
        manager.createNotificationChannel(claims);
        manager.createNotificationChannel(sync);
    }

    /**
     * {@code ITEM_MATCH}: opens the matched item.
     */
    public static void showItemMatch(@NonNull Context context,
                                     @NonNull String title,
                                     @NonNull String body,
                                     @NonNull Map<String, String> data) {
        Intent intent = new Intent(context, ItemDetailsActivity.class);
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_ID, data.get("itemId"));
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_TITLE, data.get("itemTitle"));
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_DESCRIPTION, data.get("itemDescription"));
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_LOCATION, data.get("itemLocation"));
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_STATUS, data.get("itemStatus"));
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_CATEGORY, data.get("itemCategory"));
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_IMAGE_URL, data.get("itemImageUrl"));
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_TYPE, data.get("itemType"));
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_VERIFICATION_QUESTION,
                data.get("itemVerificationQuestion"));
        intent.putExtra(ItemDetailsActivity.EXTRA_ITEM_OWNER_ID, data.get("itemOwnerId"));
        post(context, CHANNEL_MATCHES, title, body, intent);
    }

    /**
     * {@code CLAIM_PENDING}: opens home so the owner sees the claim card.
     */
    public static void showClaimPending(@NonNull Context context,
                                        @NonNull String title,
                                        @NonNull String body) {
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.putExtra(DashboardActivity.EXTRA_FOCUS_HOME, true);
        post(context, CHANNEL_CLAIMS, title, body, intent);
    }

    /**
     * {@code CLAIM_DECISION}: opens home so the claimant sees the result.
     */
    public static void showClaimDecision(@NonNull Context context,
                                         @NonNull String title,
                                         @NonNull String body) {
        showClaimPending(context, title, body);
    }

    /** Local notice: a queued report reached the server. */
    public static void showReportSynced(@NonNull Context context,
                                        @Nullable String reportTitle) {
        String body = context.getString(
                R.string.notif_report_synced_body,
                reportTitle != null ? reportTitle : "");
        post(context, CHANNEL_SYNC,
                context.getString(R.string.notif_report_synced_title),
                body,
                new Intent(context, MyItemsActivity.class));
    }

    private static void post(@NonNull Context context,
                             @NonNull String channelId,
                             @NonNull String title,
                             @NonNull String body,
                             @NonNull Intent target) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled by user; dropping \"" + title + "\"");
            return;
        }

        target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID.incrementAndGet(),
                target,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            manager.notify(NOTIFICATION_ID.incrementAndGet(), builder.build());
        } catch (SecurityException e) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; dropping \"" + title + "\"", e);
        }
    }
}
