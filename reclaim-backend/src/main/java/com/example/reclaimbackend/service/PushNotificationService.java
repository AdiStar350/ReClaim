package com.example.reclaimbackend.service;

import com.example.reclaimbackend.model.Claim;
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
 * Sends push messages to user devices.
 * The app builds the notification. Sends are in the background and
 * are skipped if Firebase is not set up.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    /** A new item that may match one of the user's reports. */
    public static final String TYPE_ITEM_MATCH = "ITEM_MATCH";

    /** Someone claimed the user's item and needs a review. */
    public static final String TYPE_CLAIM_PENDING = "CLAIM_PENDING";

    /** The owner approved or declined the user's claim. */
    public static final String TYPE_CLAIM_DECISION = "CLAIM_DECISION";

    /**
     * Tells the owner of {@code existingItem} that {@code newItem} may be a match.
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
     * Tells the owner a new claim is waiting for them to review.
     */
    public void notifyClaimPending(User owner, Item item, Claim claim) {
        Map<String, String> data = new HashMap<>();
        data.put("type", TYPE_CLAIM_PENDING);
        data.put("title", "New claim on \"" + item.getTitle() + "\"");
        data.put("body", "Someone says this item is theirs. "
                + "Open the app to review their answer and approve or decline.");
        data.put("itemId", safe(item.getId()));
        data.put("itemTitle", safe(item.getTitle()));
        data.put("claimId", claim != null ? safe(claim.getId()) : "");
        data.put("validationAnswer", claim != null ? safe(claim.getValidationAnswer()) : "");
        data.put("verificationQuestion", safe(item.getVerificationQuestion()));

        send(owner, data);
    }

    /**
     * Tells the claimant the owner approved or declined their claim.
     */
    public void notifyClaimDecision(User claimant, Item item, Claim claim, boolean approved) {
        String itemTitle = item != null ? item.getTitle() : "";
        String title = approved
                ? "Your claim was approved"
                : "Your claim was declined";
        String body = approved
                ? "The owner approved your claim on \"" + itemTitle
                        + "\". Open the app to view their contact info."
                : "The owner declined your claim on \"" + itemTitle + "\".";

        Map<String, String> data = new HashMap<>();
        data.put("type", TYPE_CLAIM_DECISION);
        data.put("title", title);
        data.put("body", body);
        data.put("claimId", claim != null ? safe(claim.getId()) : "");
        data.put("itemId", item != null ? safe(item.getId()) : "");
        data.put("itemTitle", safe(itemTitle));
        data.put("decision", approved ? "APPROVED" : "REJECTED");

        send(claimant, data);
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
