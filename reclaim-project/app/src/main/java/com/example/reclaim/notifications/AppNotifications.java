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
import com.example.reclaim.ui.details.ItemDetailsActivity;
import com.example.reclaim.ui.myitems.MyItemsActivity;
import com.example.reclaim.ui.profile.PendingClaimsActivity;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Notification channels and builders for the three push scenarios:
 * item matches, pending claims, and offline-report sync confirmations.
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

    /** Creates all channels; safe to call repeatedly. */
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
     * Push payload {@code type=ITEM_MATCH}: opens the matched item's
     * details screen so the user can review and claim it.
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
        post(context, CHANNEL_MATCHES, title, body, intent);
    }

    /**
     * Push payload {@code type=CLAIM_PENDING}: opens the pending claims
     * screen where the owner validates the claimant's answer.
     */
    public static void showClaimPending(@NonNull Context context,
                                        @NonNull String title,
                                        @NonNull String body) {
        post(context, CHANNEL_CLAIMS, title, body,
                new Intent(context, PendingClaimsActivity.class));
    }

    /** Local notification: a queued offline report reached the server. */
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
