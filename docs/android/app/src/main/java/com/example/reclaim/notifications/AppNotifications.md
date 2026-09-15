```java
private AppNotifications() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep channel and post helpers static.
Where it works: I am not called. Java uses me when someone tries `new AppNotifications()`.
Why I chose to write the code of the function the way its written: I made the constructor private so ReclaimApp calls createChannels.
More: I use a private constructor. The class is final.

```java
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
```

What that function does: I create the matches, claims, and sync notification channels.
What the function is responsible for: I am channel setup. Fine to call more than once.
Where it works: I am called by ReclaimApp.onCreate and ReclaimMessagingService.onMessageReceived.
Why I chose to write the code of the function the way its written: Matches and claims are HIGH so they alert. Sync is DEFAULT so a queued send is quieter.
More: I use NotificationChannel and string resources for names.

```java
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
```

What that function does: I show a match notice that opens the matched item.
What the function is responsible for: I am the ITEM_MATCH and TARGETED_FOUND notification.
Where it works: I am called by ReclaimMessagingService.onMessageReceived.
Why I chose to write the code of the function the way its written: I copy push data into ItemDetailsActivity extras so a tap can open details even if the list is stale.
More: I use ItemDetailsActivity extras and post on CHANNEL_MATCHES.

```java
public static void showClaimPending(@NonNull Context context,
                                    @NonNull String title,
                                    @NonNull String body) {
    Intent intent = new Intent(context, DashboardActivity.class);
    intent.putExtra(DashboardActivity.EXTRA_FOCUS_HOME, true);
    post(context, CHANNEL_CLAIMS, title, body, intent);
}
```

What that function does: I show a claim notice that opens home.
What the function is responsible for: I am the CLAIM_PENDING notification, and the generic fallback.
Where it works: I am called by ReclaimMessagingService for CLAIM_PENDING and unknown types, and by showClaimDecision.
Why I chose to write the code of the function the way its written: Home already shows the pending-claim card, so I open DashboardActivity with EXTRA_FOCUS_HOME.
More: I use DashboardActivity.EXTRA_FOCUS_HOME and CHANNEL_CLAIMS.

```java
public static void showClaimDecision(@NonNull Context context,
                                     @NonNull String title,
                                     @NonNull String body) {
    showClaimPending(context, title, body);
}
```

What that function does: I show a decision notice the same way as a pending claim.
What the function is responsible for: I am the CLAIM_DECISION notification.
Where it works: I am called by ReclaimMessagingService.onMessageReceived.
Why I chose to write the code of the function the way its written: Home also shows the decision dialog, so I reuse the same tap target.
More: I call showClaimPending.

```java
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
```

What that function does: I show a local notice that a queued report reached the server.
What the function is responsible for: I am the offline-sync success notification.
Where it works: I am called by ReportSyncWorker.syncOne after a successful POST.
Why I chose to write the code of the function the way its written: I open MyItemsActivity so the user can see the new report. An empty title still shows a body.
More: I use string resources and CHANNEL_SYNC.

```java
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
```

What that function does: I post one notification, or I drop it if notices are off.
What the function is responsible for: I am the shared builder for all four show methods.
Where it works: I am called by showItemMatch, showClaimPending, and showReportSynced.
Why I chose to write the code of the function the way its written: I catch SecurityException for missing POST_NOTIFICATIONS. FLAG_IMMUTABLE is required on newer Android.
More: I use NotificationManagerCompat, NotificationCompat.Builder, BigTextStyle, and an AtomicInteger for ids.
