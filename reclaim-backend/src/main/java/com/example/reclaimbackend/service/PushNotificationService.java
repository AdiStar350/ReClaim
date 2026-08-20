package com.example.reclaimbackend.service;

import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.model.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Sends FCM push notifications to user devices.
 * <p>
 * Messages are <b>data-only</b> so the Android client builds the
 * notification itself and behaves identically in foreground and
 * background. Sends are asynchronous and never fail the calling
 * request; when Firebase Admin is not configured they are skipped.
 * </p>
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    /** Payload type: an item matching one of the user's reports was posted. */
    public static final String TYPE_ITEM_MATCH = "ITEM_MATCH";

    /** Payload type: someone claimed the user's item; validation required. */
    public static final String TYPE_CLAIM_PENDING = "CLAIM_PENDING";

    /**
     * Notifies the owner of {@code existingItem} that {@code newItem}
     * (opposite type, same category) may be the item they reported.
     */
    public void notifyItemMatch(User recipient, Item existingItem, Item newItem) {
        boolean newIsFound = "FOUND".equalsIgnoreCase(newItem.getType());
        String title = newIsFound
                ? "Possible match for your lost item"
                : "Someone lost an item like one you found";
        String body = newIsFound
                ? "A found \"" + newItem.getTitle() + "\" may match your lost \""
                        + existingItem.getTitle() + "\". Tap to review it."
                : "A lost \"" + newItem.getTitle() + "\" may match the \""
                        + existingItem.getTitle() + "\" you found. Tap to review it.";

        Map<String, String> data = new HashMap<>();
        data.put("type", TYPE_ITEM_MATCH);
        data.put("title", title);
        data.put("body", body);
        data.put("itemId", safe(newItem.getId()));
        data.put("itemTitle", safe(newItem.getTitle()));
        data.put("itemDescription", safe(newItem.getDescription()));
        data.put("itemLocation", safe(newItem.getLocation()));
        data.put("itemStatus", safe(newItem.getStatus()));
        data.put("itemCategory", safe(newItem.getCategory()));
        data.put("itemType", safe(newItem.getType()));
        data.put("itemImageUrl", safe(newItem.getImageUrl()));
        data.put("itemVerificationQuestion", safe(newItem.getVerificationQuestion()));

        send(recipient, data);
    }

    /**
     * Notifies an item's owner that a new ownership claim is waiting
     * for their validation.
     */
    public void notifyClaimPending(User owner, Item item) {
        Map<String, String> data = new HashMap<>();
        data.put("type", TYPE_CLAIM_PENDING);
        data.put("title", "New claim on \"" + item.getTitle() + "\"");
        data.put("body", "Someone says this item is theirs. "
                + "Review their verification answer to approve or reject the claim.");
        data.put("itemId", safe(item.getId()));
        data.put("itemTitle", safe(item.getTitle()));

        send(owner, data);
    }

    private void send(User recipient, Map<String, String> data) {
        if (recipient == null || recipient.getFcmToken() == null
                || recipient.getFcmToken().isBlank()) {
            log.debug("Skipping push; user has no FCM token");
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Skipping push; Firebase Admin is not initialized");
            return;
        }

        String token = recipient.getFcmToken();
        Message message = Message.builder()
                .setToken(token)
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                String id = FirebaseMessaging.getInstance().send(message);
                log.info("Push sent type={} messageId={}", data.get("type"), id);
            } catch (Exception e) {
                log.warn("Push send failed type={}: {}", data.get("type"), e.getMessage());
            }
        });
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
