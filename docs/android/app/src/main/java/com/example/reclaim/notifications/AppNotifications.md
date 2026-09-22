# AppNotifications.java

## Overview
This file is a **static helper** that creates Android notification channels and builds
notifications for item matches, claim updates, and offline-report sync success. Each
`show*` method builds an Intent to the right screen, then `post` attaches a
PendingIntent and posts through `NotificationManagerCompat`, respecting the user's
notification permission.

## Concepts used here
- **Notification channel.** Android 8+ grouping for importance and user settings;
  matches / claims / sync each have their own channel id.
- **Intent / Activity.** Tapping a notification opens a target screen with extras; see
  `docs/GLOSSARY.md`.
- **PendingIntent.** A token the system can fire later to start that Activity.
- **`Context`.** Needed for strings, managers, and intents; see `docs/GLOSSARY.md`.
- **Toast vs notification.** These are status-bar notifications, not Toasts; see
  Toast in `docs/GLOSSARY.md` for the related idea of short user messages.
- **Static utility class.** Private constructor; shared id counter.

## Libraries and imports
- **Android (`NotificationChannel`, `NotificationManager`, `PendingIntent`, `Context`,
  `Intent`, `Log`).** Channels, posting, tap targets.
- **AndroidX (`NonNull`, `Nullable`, `NotificationCompat`, `NotificationManagerCompat`).**
  Compat builders and permission-safe notify.
- **Java (`Map`, `AtomicInteger`).** Push data extras and unique notification ids.
- **Project classes.** `R` strings/drawable; `DashboardActivity`, `ItemDetailsActivity`,
  `MyItemsActivity` — tap destinations.

## Fields
- `private static final String TAG = "AppNotifications"` — log tag.
- `public static final String CHANNEL_MATCHES = "matches"` — channel id for item-match
  pushes.
- `public static final String CHANNEL_CLAIMS = "claims"` — channel id for claim pushes.
- `public static final String CHANNEL_SYNC = "sync"` — channel id for local sync notices.
- `private static final AtomicInteger NOTIFICATION_ID` — increments so each notify /
  PendingIntent request code is unique (seeded from time).

## Functions

### `private AppNotifications()`
```java
private AppNotifications() {
}
```
- **What it does:** empty private constructor blocking `new AppNotifications()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** static-only helper.
- **Step by step:** 1) private access; 2) empty body; 3) no instances from outside.

### `private static void post(...)`
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
- **What it does:** shared poster — checks if notifications are allowed, wraps the
  target Intent in a PendingIntent, builds a high-priority notification, and notifies.
- **Where it's called:** by every public `show*` method in this class.
- **Why it's written this way:** one place owns flags, icons, and permission handling;
  catching `SecurityException` covers Android 13+ runtime permission denials.
- **Step by step:**
  1. Get `NotificationManagerCompat`; return if notifications are disabled.
  2. Set NEW_TASK | CLEAR_TOP on the target Intent.
  3. Build an immutable/updateable PendingIntent with a unique request code.
  4. Build the notification (small icon, title, body, BigText, auto-cancel, tap
     intent, high priority).
  5. `notify` with a new id; on `SecurityException`, log and drop.

### `public static void createChannels(@NonNull Context context)`
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
- **What it does:** registers the three notification channels (safe to call more than
  once).
- **Where it's called:** `ReclaimApp.onCreate` and `ReclaimMessagingService` before
  showing a push notification.
- **Why it's written this way:** channels must exist before posting on modern Android;
  calling early and again before a push is idempotent and defensive.
- **Step by step:**
  1. Get `NotificationManager`; return if null.
  2. Build matches and claims (HIGH) and sync (DEFAULT) with localized names/descriptions.
  3. `createNotificationChannel` for each.

### `public static void showItemMatch(...)`
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
- **What it does:** shows an `ITEM_MATCH` / targeted-found notification that opens
  item details with extras copied from the push data map.
- **Where it's called:** `ReclaimMessagingService` for `ITEM_MATCH` and
  `TARGETED_FOUND`.
- **Why it's written this way:** stuffing extras into the Intent lets the details
  screen open even before a fresh network fetch.
- **Step by step:** 1) Intent to `ItemDetailsActivity`; 2) copy known data keys into
  extras; 3) `post` on `CHANNEL_MATCHES`.

### `public static void showClaimPending(...)`
```java
public static void showClaimPending(@NonNull Context context,
                                    @NonNull String title,
                                    @NonNull String body) {
    Intent intent = new Intent(context, DashboardActivity.class);
    intent.putExtra(DashboardActivity.EXTRA_FOCUS_HOME, true);
    post(context, CHANNEL_CLAIMS, title, body, intent);
}
```
- **What it does:** shows a claim-pending notification that opens the dashboard focused
  on Home (where the claim card lives).
- **Where it's called:** `ReclaimMessagingService` for `CLAIM_PENDING`, and as the
  fallback for unknown push types; also reused by `showClaimDecision`.
- **Why it's written this way:** owners review claims on Home, so the tap target is
  the dashboard with a focus flag rather than a deep details link.
- **Step by step:** 1) Intent to `DashboardActivity` with `EXTRA_FOCUS_HOME`; 2)
  `post` on `CHANNEL_CLAIMS`.

### `public static void showClaimDecision(...)`
```java
public static void showClaimDecision(@NonNull Context context,
                                     @NonNull String title,
                                     @NonNull String body) {
    showClaimPending(context, title, body);
}
```
- **What it does:** shows a claim-decision notification with the same Home destination
  as pending claims.
- **Where it's called:** `ReclaimMessagingService` for `CLAIM_DECISION`.
- **Why it's written this way:** claimants also land on Home to see the result, so
  reusing `showClaimPending` avoids a second nearly identical builder.
- **Step by step:** 1) delegate to `showClaimPending`.

### `public static void showReportSynced(...)`
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
- **What it does:** local notice that a queued offline report reached the server;
  tap opens My Items.
- **Where it's called:** `ReportSyncWorker.syncOne` after a successful `createItem`.
- **Why it's written this way:** sync success is not an FCM push; the worker posts
  locally so the user knows the queue cleared. Null title becomes an empty string in
  the body format.
- **Step by step:** 1) format body with the report title; 2) `post` on `CHANNEL_SYNC`
  with title string and Intent to `MyItemsActivity`.
