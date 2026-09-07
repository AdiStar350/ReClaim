package com.example.reclaim.notifications;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.reclaim.R;
import com.example.reclaim.data.AppDataRefresh;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Incoming push messages from the backend.
 * Data-only, so this service builds the notification itself.
 */
public class ReclaimMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ReclaimMessaging";

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "FCM token rotated");
        FcmTokenRegistrar.sendToken(getApplicationContext(), token);
    }

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
                || "CLAIM_PENDING".equals(type)
                || "CLAIM_DECISION".equals(type)) {
            AppDataRefresh.notifyChanged();
        }

        if ("ITEM_MATCH".equals(type)) {
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
}
