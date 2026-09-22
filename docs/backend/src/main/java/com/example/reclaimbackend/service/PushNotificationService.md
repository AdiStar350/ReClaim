# PushNotificationService.java

## Overview
This is a **service** that sends **FCM** data messages to user devices (see `docs/GLOSSARY.md`). The Android app builds the visible notification from the data payload. Sends run asynchronously and are skipped when the user has no FCM token or Firebase Admin was not initialized, so missing push setup never blocks claim/item saves.

## Concepts used here
- **Service** — notification helper used by item/claim flows; see `docs/GLOSSARY.md`.
- **Firebase / FCM** — cloud messaging to devices; see `docs/GLOSSARY.md`.
- **Data message** — key/value map the app interprets (`type`, `title`, `body`, ids).
- **Async send** — `CompletableFuture.runAsync` so HTTP requests do not wait on FCM.

## Libraries and imports
- **Firebase Messaging** (`FirebaseApp`, `FirebaseMessaging`, `Message`, `AndroidConfig`): build and send high-priority Android data messages.
- **Spring** (`@Service`): registers the bean.
- **SLF4J**: debug skips and warn on send failure.
- **Java concurrency / collections**: async send and data maps.
- **Project models**: `User`, `Item`, `Claim` supply tokens and payload fields.

## Fields
- `log` (`Logger`) — static logger.
- `TYPE_ITEM_MATCH` / `TYPE_TARGETED_FOUND` / `TYPE_CLAIM_PENDING` / `TYPE_CLAIM_DECISION` (`String`) — `type` values the Android app switches on.

## Functions

### `private static String safe(String value)`
```java
private static String safe(String value) {
    return value != null ? value : "";
}
```
- **What it does:** turns null strings into empty strings for FCM data maps (which disallow null values).
- **Where it's called:** all public `notify*` methods in this class when filling the data map.
- **Why it's written this way:** FCM `putAllData` requires non-null string values.
- **Step by step:**
  1. If value is null, return `""`.
  2. Otherwise return the value.

### `private void send(User recipient, Map<String, String> data)`
```java
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
```
- **What it does:** builds a high-priority data message and sends it in a background thread, or skips quietly when token/Firebase is missing.
- **Where it's called:** every public `notify*` method in this class.
- **Why it's written this way:** async + catch means a slow or failed FCM call never rolls back the business action that already saved.
- **Step by step:**
  1. Return if recipient/token missing.
  2. Return if Firebase Admin has no apps.
  3. Build `Message` with token, data, and HIGH Android priority.
  4. `runAsync` to send; log success id or warn on failure.

### `public void notifyItemMatch(User recipient, Item existingItem, Item newItem)`
```java
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
```
- **What it does:** notifies an owner that a new opposite-type report may match one of theirs.
- **Where it's called:** `ItemService.notifyOwnersOfPotentialMatches`.
- **Why it's written this way:** wording depends on whether the new item is FOUND or LOST; payload includes enough item fields for the app UI.
- **Step by step:**
  1. Choose title/body based on `newItem` type.
  2. Fill a data map with `TYPE_ITEM_MATCH` and item details.
  3. Call `send`.

### `public void notifyTargetedFound(User lostOwner, Item lostItem, Item foundItem)`
```java
public void notifyTargetedFound(User lostOwner, Item lostItem, Item foundItem) {
    String foundTitle = foundItem != null ? foundItem.getTitle() : "";
    String lostTitle = lostItem != null ? lostItem.getTitle() : "";
    String title = "Someone found your item";
    String body = "A found \"" + foundTitle + "\" was sent for your lost \""
            + lostTitle + "\". Tap to review it.";

    Map<String, String> data = new HashMap<>();
    data.put("type", TYPE_TARGETED_FOUND);
    data.put("title", title);
    data.put("body", body);
    data.put("itemId", safe(foundItem != null ? foundItem.getId() : null));
    data.put("itemTitle", safe(foundTitle));
    data.put("itemDescription", safe(foundItem != null ? foundItem.getDescription() : null));
    data.put("itemLocation", safe(foundItem != null ? foundItem.getLocation() : null));
    data.put("itemStatus", safe(foundItem != null ? foundItem.getStatus() : null));
    data.put("itemCategory", safe(foundItem != null ? foundItem.getCategory() : null));
    data.put("itemType", safe(foundItem != null ? foundItem.getType() : null));
    data.put("itemImageUrl", safe(foundItem != null ? foundItem.getImageUrl() : null));
    data.put("itemVerificationQuestion",
            safe(foundItem != null ? foundItem.getVerificationQuestion() : null));
    data.put("itemOwnerId", safe(foundItem != null ? foundItem.getOwnerId() : null));

    send(lostOwner, data);
}
```
- **What it does:** tells the lost-item owner that someone filed a private/targeted found report for them.
- **Where it's called:** `ItemService.notifyTargetedLostOwner`.
- **Why it's written this way:** separate type from generic matches so the app can open the right screen.
- **Step by step:**
  1. Build title/body from lost and found titles.
  2. Put `TYPE_TARGETED_FOUND` and found-item fields into the map.
  3. Call `send` for the lost owner.

### `public void notifyClaimPending(User owner, Item item, Claim claim)`
```java
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
```
- **What it does:** tells the item owner a new claim needs review, including the validation answer.
- **Where it's called:** `ClaimService.submitClaim`.
- **Why it's written this way:** includes claim and question fields so the notification UI can preview the answer.
- **Step by step:**
  1. Build data with `TYPE_CLAIM_PENDING`.
  2. Add item/claim fields safely.
  3. Call `send` for the owner.

### `public void notifyClaimDecision(User claimant, Item item, Claim claim, boolean approved)`
```java
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
```
- **What it does:** tells the claimant whether their claim was approved or rejected.
- **Where it's called:** `ClaimService.notifyClaimantOfDecision` (from `reviewClaim`, including auto-rejected siblings).
- **Why it's written this way:** `decision` field lets the app branch without parsing the English body text.
- **Step by step:**
  1. Choose approved vs declined title/body.
  2. Fill `TYPE_CLAIM_DECISION` data including `decision`.
  3. Call `send` for the claimant.
