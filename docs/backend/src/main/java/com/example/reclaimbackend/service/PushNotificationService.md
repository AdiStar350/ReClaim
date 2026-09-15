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

What that function does: I tell the owner of an existing report that a new item may match it.
What the function is responsible for: I build an ITEM_MATCH data payload and send it.
Where it works: I am called by ItemService.notifyOwnersOfPotentialMatches.
Why I chose to write the code of the function the way its written: I change the title and body based on whether the new item is found or lost. I put item fields in data so the app can show them.
More: I use HashMap, TYPE_ITEM_MATCH, and safe so null fields become empty strings.

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

What that function does: I tell the lost-item owner that someone filed a private found report for them.
What the function is responsible for: I build a TARGETED_FOUND payload and send it.
Where it works: I am called by ItemService.notifyTargetedLostOwner.
Why I chose to write the code of the function the way its written: I null-check foundItem and lostItem so a partial call does not throw. I include itemOwnerId for the app.
More: I use TYPE_TARGETED_FOUND and send.

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

What that function does: I tell the owner a new claim is waiting for review.
What the function is responsible for: I build a CLAIM_PENDING payload and send it.
Where it works: I am called by ClaimService.submitClaim.
Why I chose to write the code of the function the way its written: I include the answer and question so the owner can review from the notification data if the app wants that.
More: I use TYPE_CLAIM_PENDING and a null-safe claim id.

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

What that function does: I tell the claimant the owner approved or declined their claim.
What the function is responsible for: I build a CLAIM_DECISION payload and send it.
Where it works: I am called by ClaimService.notifyClaimantOfDecision from reviewClaim.
Why I chose to write the code of the function the way its written: I change title and body on approved. I add a decision field so the app does not have to parse the title.
More: I use TYPE_CLAIM_DECISION and send.

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

What that function does: I send the data map to the user's device in the background, or skip if there is no token or Firebase.
What the function is responsible for: I am the shared FCM send. A failed send does not throw to the caller.
Where it works: I am called by notifyItemMatch, notifyTargetedFound, notifyClaimPending, and notifyClaimDecision. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because runAsync keeps item and claim saves from waiting on FCM. HIGH priority helps Android show it sooner.
More: I use FirebaseMessaging, Message, AndroidConfig, CompletableFuture, and FirebaseApp.getApps.

```java
private static String safe(String value) {
    return value != null ? value : "";
}
```

What that function does: I turn null into an empty string so FCM data maps stay valid.
What the function is responsible for: I am a small null guard for payload fields.
Where it works: I am called by the notify methods. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because fCM data values cannot be null, so empty string is the simple stand-in.
More: I use no extra libraries.
