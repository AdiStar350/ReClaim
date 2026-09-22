# ReclaimMessagingService.java

## Overview
This file is the app's **Firebase Messaging Service**. It receives data-only push
messages from the backend, builds the on-device notification itself, and fans out
side effects (refresh signals for matches/claims). Android starts this service when a
message arrives or when the FCM token rotates; it is declared in
`AndroidManifest.xml`.

## Concepts used here
- **Firebase / FCM.** Push delivery; `RemoteMessage` carries a data map; see
  `docs/GLOSSARY.md`.
- **Notification channels (indirect).** `AppNotifications.createChannels` ensures
  channels exist before posting; see Toast/notifications in `docs/GLOSSARY.md`.
- **LiveData events (indirect).** `IncomingClaimEvents` / `ClaimDecisionEvents` wake
  Home while the app is open.
- **Service.** A long-lived Android component (here, Firebase's messaging subclass)
  that is not a full Activity screen.

## Libraries and imports
- **Android (`Log`).** Debug/warn logging.
- **AndroidX annotations (`NonNull`).** Required overrides.
- **Firebase Messaging (`FirebaseMessagingService`, `RemoteMessage`).** Base class and
  incoming payload.
- **Java (`Map`).** The data payload key/value map.
- **Project classes.** `R.string.app_name` — fallback title; `AppStateEvents` —
  mutation signal for list refresh; `AppNotifications` — show typed notifications;
  `IncomingClaimEvents` / `ClaimDecisionEvents` — in-app LiveData pings;
  `FcmTokenRegistrar` — re-register rotated tokens.

## Fields
- `private static final String TAG = "ReclaimMessaging"` — log tag.

## Functions

### `public void onNewToken(@NonNull String token)`
```java
@Override
public void onNewToken(@NonNull String token) {
    Log.d(TAG, "FCM token rotated");
    FcmTokenRegistrar.sendToken(getApplicationContext(), token);
}
```
- **What it does:** when Firebase issues a new device token, sends it to the backend.
- **Where it's called:** by the Firebase SDK when the FCM token is created or
  rotated (system callback, not app Java).
- **Why it's written this way:** without re-registering, the backend would keep an
  old token and pushes would miss this device.
- **Step by step:** 1) log rotation; 2) call `FcmTokenRegistrar.sendToken` with the
  app context and new token.

### `public void onMessageReceived(@NonNull RemoteMessage message)`
```java
@Override
public void onMessageReceived(@NonNull RemoteMessage message) {
    Map<String, String> data = message.getData();
    String type = data.get("type");
    Log.d(TAG, "Push received type=" + type + " keys=" + data.keySet());

    AppNotifications.createChannels(this);

    String title = data.get("title");
    String body = data.get("body");

    // Fallback if the payload has a notification block.
    if ((title == null || body == null) && message.getNotification() != null) {
        if (title == null) {
            title = message.getNotification().getTitle();
        }
        if (body == null) {
            body = message.getNotification().getBody();
        }
    }
    if (title == null) {
        title = getString(R.string.app_name);
    }
    if (body == null) {
        body = "";
    }

    if ("ITEM_MATCH".equals(type)
            || "TARGETED_FOUND".equals(type)
            || "CLAIM_PENDING".equals(type)
            || "CLAIM_DECISION".equals(type)) {
        AppStateEvents.notifyMutation();
    }

    if ("ITEM_MATCH".equals(type) || "TARGETED_FOUND".equals(type)) {
        AppNotifications.showItemMatch(this, title, body, data);
    } else if ("CLAIM_PENDING".equals(type)) {
        IncomingClaimEvents.notifyReceived();
        AppNotifications.showClaimPending(this, title, body);
    } else if ("CLAIM_DECISION".equals(type)) {
        ClaimDecisionEvents.notifyReceived();
        AppNotifications.showClaimDecision(this, title, body);
    } else {
        Log.w(TAG, "Unknown push type=" + type + "; showing generic notification");
        AppNotifications.showClaimPending(this, title, body);
    }
}
```
- **What it does:** reads the push `type`, ensures notification channels exist, fills
  title/body (with fallbacks), notifies app state / claim event buses for known types,
  and shows the matching notification.
- **Where it's called:** by the Firebase SDK when a data message arrives while the
  service can handle it (system callback).
- **Why it's written this way:** data-only pushes mean the app must build the
  notification; known types also poke LiveData/AppState so open screens refresh;
  unknown types still show something useful instead of silent failure.
- **Step by step:**
  1. Read the data map and `type`; log keys.
  2. Create notification channels.
  3. Read title/body from data; fall back to the notification block, then app name /
     empty body.
  4. For match/claim types, call `AppStateEvents.notifyMutation()`.
  5. Branch on type: match → `showItemMatch`; pending → event + `showClaimPending`;
     decision → event + `showClaimDecision`; else warn and show a generic pending-style
     notification.
