package com.example.reclaim.notifications;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.reclaim.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Receives FCM messages from the ReClaim backend.
 * <p>
 * The backend sends <b>data-only</b> messages so this service is invoked
 * in both foreground and background and builds the notification itself.
 * Supported {@code type} values:
 * <ul>
 *   <li>{@code ITEM_MATCH} — a lost/found item matching one of the user's
 *       reports was posted; taps open the matched item's details.</li>
 *   <li>{@code CLAIM_PENDING} — someone claimed the user's item and the
 *       answer needs validation; taps open the pending claims screen.</li>
 * </ul>
 * </p>
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

        // Fallback for notification-type payloads (e.g. console test sends).
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

        if ("ITEM_MATCH".equals(type)) {
            AppNotifications.showItemMatch(this, title, body, data);
        } else if ("CLAIM_PENDING".equals(type)) {
            AppNotifications.showClaimPending(this, title, body);
        } else {
            Log.w(TAG, "Unknown push type=" + type + "; showing generic notification");
            AppNotifications.showClaimPending(this, title, body);
        }
    }
}
