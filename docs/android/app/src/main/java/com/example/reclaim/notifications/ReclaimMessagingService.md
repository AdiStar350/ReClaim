```java
@Override
public void onNewToken(@NonNull String token) {
    Log.d(TAG, "FCM token rotated");
    FcmTokenRegistrar.sendToken(getApplicationContext(), token);
}
```

What that function does: I send a rotated FCM token to the backend.
What the function is responsible for: I am Firebase's token-refresh callback.
Where it works: AndroidManifest names me as ReclaimMessagingService. Firebase calls me when the token changes.
Why I chose to write the code of the function the way its written: I reuse FcmTokenRegistrar so start-up and rotation share one PUT.
More: I use FcmTokenRegistrar.sendToken and getApplicationContext.

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

What that function does: I build a local notification from a data push and tick home when needed.
What the function is responsible for: I am the incoming FCM handler. Data-only messages do not auto-show.
Where it works: Firebase calls me while the process is running. AndroidManifest lists me as the messaging service.
Why I chose to write the code of the function the way its written: I fall back to the notification block and the app name so a thin payload still shows something. Known types also notify AppStateEvents so lists refresh.
More: I use RemoteMessage.getData, AppNotifications, IncomingClaimEvents, ClaimDecisionEvents, and AppStateEvents.notifyMutation.
