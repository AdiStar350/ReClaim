# ItemService.java

## Overview
This is a **service** for lost/found **items**: list, search, create/update/close/delete, matching, targeted found reports, and owner contact (see `docs/GLOSSARY.md`). It enriches responses with owner display fields, hides private targeted found reports from public feeds, and fires **FCM** match/targeted notifications after create. Image URL changes delete the previous file through `FirebaseStorageService`.

## Concepts used here
- **Service** — item logic behind `ItemController`; see `docs/GLOSSARY.md`.
- **MongoDB / MongoTemplate** — dynamic search queries; see `docs/GLOSSARY.md`.
- **Model / repository** — `Item` documents and related claims/users; see `docs/GLOSSARY.md`.
- **Authorization / visibility** — private targeted found reports only for finder or lost owner; see `docs/GLOSSARY.md`.
- **Firebase Storage / FCM** — image cleanup and match pushes; see `docs/GLOSSARY.md`.
- **DTO** — `ContactResponse` for lost-owner contact; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Data MongoDB** (`MongoTemplate`, `Criteria`, `Query`, `Sort`): filtered search.
- **Spring** (`@Service`, `StringUtils`, `HttpStatus`, `ResponseStatusException`): service bean and API errors.
- **Lombok** (`@RequiredArgsConstructor`): DI.
- **SLF4J**: warn when notifications fail.
- **Java time/collections**: `Instant`, lists/maps/sets for matching and enrichment.
- **Project**: repos, `PushNotificationService`, `FirebaseStorageService`, models/DTOs.

## Fields
- `log` (`Logger`) — static logger.
- `itemRepository` / `claimRepository` / `userRepository` — persistence.
- `mongoTemplate` (`MongoTemplate`) — custom search queries.
- `pushNotificationService` (`PushNotificationService`) — match / targeted found pushes.
- `firebaseStorageService` (`FirebaseStorageService`) — delete replaced item images.

## Functions

### `private static String blankToNull(String value)`
```java
private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }
    return value.trim();
}
```
- **What it does:** normalizes blank strings to null.
- **Where it's called:** `updateItem` when comparing image URLs.
- **Why it's written this way:** empty and null both mean “no image.”
- **Step by step:**
  1. Null/blank → null; else trimmed value.

### `private static boolean isPrivateTargeted(Item item)`
```java
private static boolean isPrivateTargeted(Item item) {
    return item != null && StringUtils.hasText(item.getTargetLostReportId());
}
```
- **What it does:** true when the item is a found report aimed at a specific lost report.
- **Where it's called:** feed/search filters, create notifications, match lists, `canViewerSee`, and several public methods.
- **Why it's written this way:** one check for “private targeted” behavior.
- **Step by step:**
  1. Return whether `targetLostReportId` is non-blank.

### `private boolean canViewerSee(Item item, String viewerId)`
```java
private boolean canViewerSee(Item item, String viewerId) {
    if (!isPrivateTargeted(item)) {
        return true;
    }
    if (viewerId != null && viewerId.equals(item.getOwnerId())) {
        return true;
    }
    return itemRepository.findById(item.getTargetLostReportId())
            .map(lost -> viewerId != null && viewerId.equals(lost.getOwnerId()))
            .orElse(false);
}
```
- **What it does:** public items are visible to all signed-in callers; private targeted ones only to the finder or the targeted lost owner.
- **Where it's called:** `getVisibleItem`.
- **Why it's written this way:** keeps “I found your item” reports out of the general feed and strangers' detail views.
- **Step by step:**
  1. Non-targeted → true.
  2. Viewer is owner → true.
  3. Else true only if viewer owns the target lost report.

### `private double distanceKm(double lat1, double lon1, Double lat2, Double lon2)`
```java
private double distanceKm(double lat1, double lon1, Double lat2, Double lon2) {
    if (lat2 == null || lon2 == null) {
        return Double.MAX_VALUE;
    }
    double earthRadiusKm = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
}
```
- **What it does:** great-circle distance in km (Haversine); missing coords sort last via `Double.MAX_VALUE`.
- **Where it's called:** `searchItems` when latitude/longitude query params are present.
- **Why it's written this way:** map search can sort nearest first without a geo index query.
- **Step by step:**
  1. If item coords missing, return max distance.
  2. Compute Haversine and return km.

### `private Item enrichWithOwner(Item item)`
```java
private Item enrichWithOwner(Item item) {
    if (item != null && item.getOwnerId() != null) {
        userRepository.findById(item.getOwnerId()).ifPresent(owner -> {
            item.setOwnerName(owner.getName());
            item.setOwnerProfilePictureUrl(owner.getProfilePictureUrl());
        });
    }
    return item;
}
```
- **What it does:** fills transient owner display fields on one item (not persisted).
- **Where it's called:** `getVisibleItem`, `getItemById`, `closeReport`, and indirectly via list helpers.
- **Why it's written this way:** API responses can show reporter name/photo without a separate user call.
- **Step by step:**
  1. Look up owner by `ownerId`.
  2. Set name and profile picture URL on the item.
  3. Return the item.

### `private List<Item> enrichWithOwners(List<Item> items)`
```java
private List<Item> enrichWithOwners(List<Item> items) {
    Map<String, User> ownerCache = new HashMap<>();
    for (Item item : items) {
        String ownerId = item.getOwnerId();
        if (ownerId == null) {
            continue;
        }
        if (!ownerCache.containsKey(ownerId)) {
            ownerCache.put(ownerId, userRepository.findById(ownerId).orElse(null));
        }
        User owner = ownerCache.get(ownerId);
        if (owner != null) {
            item.setOwnerName(owner.getName());
            item.setOwnerProfilePictureUrl(owner.getProfilePictureUrl());
        }
    }
    return items;
}
```
- **What it does:** same as `enrichWithOwner` for a list, caching user lookups.
- **Where it's called:** `getAllItems`, `getTargetedFoundReportsForLostOwner`, `getItemsByOwner`, `getItemsByCategory`, `findMatchesForLostItem`, `searchItems`.
- **Why it's written this way:** avoids N identical user fetches when many items share an owner.
- **Step by step:**
  1. For each item, load owner into a map cache once.
  2. Copy name/picture onto the item.
  3. Return the list.

### `private boolean hasUserAlreadyInteracted(Item item, String viewerId)`
```java
private boolean hasUserAlreadyInteracted(Item item, String viewerId) {
    if (item == null || item.getId() == null || !StringUtils.hasText(viewerId)) {
        return false;
    }
    if (viewerId.equals(item.getOwnerId())) {
        return false;
    }
    if ("FOUND".equalsIgnoreCase(item.getType())) {
        return !claimRepository
                .findByItemIdAndClaimantIdAndStatus(item.getId(), viewerId, "PENDING")
                .isEmpty();
    }
    if ("LOST".equalsIgnoreCase(item.getType())) {
        return !itemRepository
                .findByOwnerIdAndTargetLostReportId(viewerId, item.getId())
                .isEmpty();
    }
    return false;
}
```
- **What it does:** true if the viewer already has a pending claim on a found item, or already filed a targeted found against a lost item.
- **Where it's called:** `enrichViewerInteraction`.
- **Why it's written this way:** the details UI can disable “claim again” / “I found this” when already done.
- **Step by step:**
  1. Guard nulls; owners never count as interacted.
  2. FOUND → any PENDING claim by viewer.
  3. LOST → any found report by viewer targeting this lost id.

### `private Item enrichViewerInteraction(Item item, String viewerId)`
```java
private Item enrichViewerInteraction(Item item, String viewerId) {
    item.setHasUserAlreadyInteracted(hasUserAlreadyInteracted(item, viewerId));
    return item;
}
```
- **What it does:** sets `hasUserAlreadyInteracted` on the item for the response.
- **Where it's called:** `getVisibleItem`.
- **Why it's written this way:** keeps the flag computation in one place for detail views.
- **Step by step:**
  1. Call `hasUserAlreadyInteracted`.
  2. Set the field and return.

### `private int titleOverlapScore(String lostTitle, String foundTitle)`
```java
private int titleOverlapScore(String lostTitle, String foundTitle) {
    if (lostTitle == null || foundTitle == null) {
        return 0;
    }
    String[] lostWords = lostTitle.toLowerCase().split("\\s+");
    String foundLower = foundTitle.toLowerCase();
    int score = 0;
    for (String word : lostWords) {
        if (word.length() > 2 && foundLower.contains(word)) {
            score++;
        }
    }
    return score;
}
```
- **What it does:** simple word-overlap score between two titles.
- **Where it's called:** `notifyOwnersOfPotentialMatches` and `findMatchesForLostItem` sorting.
- **Why it's written this way:** lightweight match ranking without a search engine.
- **Step by step:**
  1. Split lost title; count words length > 2 found in found title.
  2. Return score.

### `private void applyTargetedFoundRules(Item item)`
```java
private void applyTargetedFoundRules(Item item) {
    if (!StringUtils.hasText(item.getTargetLostReportId())) {
        item.setTargetLostReportId(null);
        return;
    }
    Item lost = itemRepository.findById(item.getTargetLostReportId().trim())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Lost report not found"));
    if (!"LOST".equalsIgnoreCase(lost.getType())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Target must be a lost report");
    }
    if (!"OPEN".equalsIgnoreCase(lost.getStatus())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "That lost report is already closed");
    }
    if (lost.getOwnerId() != null && lost.getOwnerId().equals(item.getOwnerId())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "You cannot file a found report against your own lost item");
    }
    item.setTargetLostReportId(lost.getId());
    item.setType("Found");
}
```
- **What it does:** validates a targeted found report's link and forces type to Found.
- **Where it's called:** `createItem`.
- **Why it's written this way:** prevents invalid/self targets before save.
- **Step by step:**
  1. Clear blank targets.
  2. Load lost report; require LOST + OPEN + different owner.
  3. Store canonical id and set type Found.

### `private void notifyTargetedLostOwner(Item foundItem)`
```java
private void notifyTargetedLostOwner(Item foundItem) {
    try {
        Item lost = itemRepository.findById(foundItem.getTargetLostReportId())
                .orElse(null);
        if (lost == null || lost.getOwnerId() == null) {
            return;
        }
        userRepository.findById(lost.getOwnerId()).ifPresent(owner ->
                pushNotificationService.notifyTargetedFound(owner, lost, foundItem));
    } catch (Exception e) {
        log.warn("Targeted found notification failed for item {}: {}",
                foundItem.getId(), e.getMessage());
    }
}
```
- **What it does:** best-effort push to the lost owner when a private found report is created.
- **Where it's called:** `createItem` when the saved item is private targeted.
- **Why it's written this way:** push must not undo the new item.
- **Step by step:**
  1. Load linked lost report and its owner.
  2. Call `notifyTargetedFound`.
  3. Log and ignore failures.

### `private void notifyOwnersOfPotentialMatches(Item newItem)`
```java
private void notifyOwnersOfPotentialMatches(Item newItem) {
    try {
        if (isPrivateTargeted(newItem)) {
            return;
        }
        if (newItem.getCategory() == null || newItem.getType() == null) {
            return;
        }
        String oppositeType = "LOST".equalsIgnoreCase(newItem.getType())
                ? "FOUND" : "LOST";

        Set<String> notifiedOwners = new HashSet<>();
        for (Item candidate : itemRepository.findByCategory(newItem.getCategory())) {
            if (!oppositeType.equalsIgnoreCase(candidate.getType())) {
                continue;
            }
            if (!"OPEN".equalsIgnoreCase(candidate.getStatus())) {
                continue;
            }
            if (isPrivateTargeted(candidate)) {
                continue;
            }
            if (candidate.getOwnerId() == null
                    || candidate.getOwnerId().equals(newItem.getOwnerId())
                    || !notifiedOwners.add(candidate.getOwnerId())) {
                continue;
            }
            if (titleOverlapScore(candidate.getTitle(), newItem.getTitle()) < 1) {
                continue;
            }
            Optional<User> owner = userRepository.findById(candidate.getOwnerId());
            owner.ifPresent(user ->
                    pushNotificationService.notifyItemMatch(user, candidate, newItem));
        }
    } catch (Exception e) {
        log.warn("Match notification failed for new item {}: {}",
                newItem.getId(), e.getMessage());
    }
}
```
- **What it does:** notifies owners of open opposite-type items in the same category with title overlap (once per owner).
- **Where it's called:** `createItem` for non-targeted new items.
- **Why it's written this way:** one push per owner even if they have several candidates.
- **Step by step:**
  1. Skip private/targeted or incomplete new items.
  2. Scan same-category open opposite-type public items.
  3. On title score ≥ 1, notify that owner once.

### `public List<Item> getAllItems(String viewerId)`
```java
public List<Item> getAllItems(String viewerId) {
    List<Item> items = itemRepository.findAll();
    items.removeIf(item -> "CLOSED".equalsIgnoreCase(item.getStatus())
            || isPrivateTargeted(item)
            || (viewerId != null && viewerId.equals(item.getOwnerId())));
    return enrichWithOwners(items);
}
```
- **What it does:** home/search-style feed: open, non-targeted, not the viewer's own items, with owner enrichment.
- **Where it's called:** `ItemController.getAllItems`.
- **Why it's written this way:** own and closed reports stay on My Items / history instead of the public feed.
- **Step by step:**
  1. Load all items.
  2. Remove closed, private targeted, and viewer's own.
  3. Enrich and return.

### `public Optional<Item> getVisibleItem(String id, String viewerId)`
```java
public Optional<Item> getVisibleItem(String id, String viewerId) {
    return itemRepository.findById(id)
            .filter(item -> canViewerSee(item, viewerId))
            .map(this::enrichWithOwner)
            .map(item -> enrichViewerInteraction(item, viewerId));
}
```
- **What it does:** returns one item if the viewer may see it, enriched with owner + interaction flag.
- **Where it's called:** `ItemController.getItemById`.
- **Why it's written this way:** empty Optional becomes HTTP 404 in the controller for both missing and forbidden private items.
- **Step by step:**
  1. Find by id.
  2. Filter with `canViewerSee`.
  3. Enrich owner and interaction.

### `public Optional<Item> getItemById(String id)`
```java
public Optional<Item> getItemById(String id) {
    return itemRepository.findById(id).map(this::enrichWithOwner);
}
```
- **What it does:** loads an item by id with owner enrichment (no visibility filter).
- **Where it's called:** **not currently called** from other production classes (controllers use `getVisibleItem`). Kept as a simple lookup helper.
- **Why it's written this way:** useful for internal/unfiltered reads; visibility is handled elsewhere.
- **Step by step:**
  1. Find by id.
  2. Map through `enrichWithOwner`.

### `public List<Item> getOpenLostItemsByOwner(String ownerId)`
```java
public List<Item> getOpenLostItemsByOwner(String ownerId) {
    return itemRepository.findByOwnerIdAndTypeIgnoreCaseAndStatusIgnoreCase(
            ownerId, "Lost", "OPEN");
}
```
- **What it does:** returns the user's open lost reports.
- **Where it's called:** `getTargetedFoundReportsForLostOwner` in this class.
- **Why it's written this way:** repository method encodes type/status filters.
- **Step by step:**
  1. Query owner + Lost + OPEN.
  2. Return the list.

### `public List<Item> getTargetedFoundReportsForLostOwner(String lostOwnerId)`
```java
public List<Item> getTargetedFoundReportsForLostOwner(String lostOwnerId) {
    List<String> lostIds = new ArrayList<>();
    for (Item owned : getOpenLostItemsByOwner(lostOwnerId)) {
        if (owned.getId() != null) {
            lostIds.add(owned.getId());
        }
    }
    if (lostIds.isEmpty()) {
        return List.of();
    }
    List<Item> targeted = itemRepository.findByTargetLostReportIdIn(lostIds);
    targeted.removeIf(item -> !"OPEN".equalsIgnoreCase(item.getStatus())
            || !"FOUND".equalsIgnoreCase(item.getType()));
    targeted.sort(Comparator.comparing(Item::getReportedAt,
            Comparator.nullsLast(Comparator.reverseOrder())));
    return enrichWithOwners(targeted);
}
```
- **What it does:** open found reports targeting this user's open lost reports, newest first.
- **Where it's called:** `ItemController.getTargetedFoundReports`.
- **Why it's written this way:** inbox for “someone found your item” without exposing those reports publicly.
- **Step by step:**
  1. Collect open lost ids.
  2. Find items targeting those ids; keep open FOUND.
  3. Sort by `reportedAt` desc; enrich.

### `public ContactResponse getLostItemOwnerContact(String itemId)`
```java
public ContactResponse getLostItemOwnerContact(String itemId) {
    Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Item not found"));
    if (!"LOST".equalsIgnoreCase(item.getType())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Contact details are only available for lost-item reports");
    }
    if (item.getOwnerId() == null) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Reporter not found");
    }
    User owner = userRepository.findById(item.getOwnerId())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Reporter not found"));
    return ContactResponse.builder()
            .name(owner.getName())
            .email(owner.getEmail())
            .phoneNumber(owner.getPhoneNumber())
            .build();
}
```
- **What it does:** returns the reporter's contact for a LOST item (found items use the claim flow instead).
- **Where it's called:** `ItemController.getLostItemOwnerContact`.
- **Why it's written this way:** finders of a lost report can reach the owner without claiming.
- **Step by step:**
  1. Load item; require LOST type.
  2. Load owner; return name/email/phone.

### `public List<Item> getItemsByOwner(String ownerId)`
```java
public List<Item> getItemsByOwner(String ownerId) {
    List<Item> items = itemRepository.findByOwnerId(ownerId);
    items.sort(Comparator.comparing(Item::getReportedAt,
            Comparator.nullsLast(Comparator.reverseOrder())));
    return enrichWithOwners(items);
}
```
- **What it does:** all of one user's items, newest first, enriched.
- **Where it's called:** `ItemController.getMyItems`.
- **Why it's written this way:** My Items includes closed reports the public feed hides.
- **Step by step:**
  1. Find by owner.
  2. Sort by `reportedAt` desc.
  3. Enrich and return.

### `public List<Item> getItemsByCategory(String category)`
```java
public List<Item> getItemsByCategory(String category) {
    List<Item> items = itemRepository.findByCategory(category);
    items.removeIf(ItemService::isPrivateTargeted);
    return enrichWithOwners(items);
}
```
- **What it does:** items in a category, excluding private targeted found reports.
- **Where it's called:** `ItemController.getItemsByCategory`.
- **Why it's written this way:** category browse should not leak private targeted reports.
- **Step by step:**
  1. Find by category.
  2. Remove private targeted.
  3. Enrich and return.

### `public Item createItem(Item item)`
```java
public Item createItem(Item item) {
    applyTargetedFoundRules(item);
    item.setStatus("OPEN");
    if (item.getReportedAt() == null) {
        item.setReportedAt(Instant.now());
    }
    Item saved = itemRepository.save(item);
    if (isPrivateTargeted(saved)) {
        notifyTargetedLostOwner(saved);
    } else {
        notifyOwnersOfPotentialMatches(saved);
    }
    return saved;
}
```
- **What it does:** validates targeted rules, saves as OPEN, then sends the appropriate push.
- **Where it's called:** `ItemController.createItem`.
- **Why it's written this way:** status/timestamp defaults live in one place; notifications are best-effort after save.
- **Step by step:**
  1. Apply targeted rules; set OPEN and `reportedAt` if needed.
  2. Save.
  3. Notify targeted owner or match owners.

### `public void deleteItem(String id, String userId)`
```java
public void deleteItem(String id, String userId) {
    Item item = itemRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Item not found"));
    if (!userId.equals(item.getOwnerId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Not allowed to delete this item");
    }
    itemRepository.deleteById(id);
}
```
- **What it does:** deletes an item only if the caller owns it.
- **Where it's called:** `ItemController.deleteItem`.
- **Why it's written this way:** ownership check before delete.
- **Step by step:**
  1. Load item or 404.
  2. Require owner or 403.
  3. Delete by id.

### `public Item updateItem(String id, String userId, Item updates)`
```java
public Item updateItem(String id, String userId, Item updates) {
    Item item = itemRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Item not found"));
    if (!userId.equals(item.getOwnerId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Not allowed to update this item");
    }

    if (updates.getTitle() != null) {
        item.setTitle(updates.getTitle());
    }
    if (updates.getDescription() != null) {
        item.setDescription(updates.getDescription());
    }
    if (updates.getCategory() != null) {
        item.setCategory(updates.getCategory());
    }
    if (updates.getLocation() != null) {
        item.setLocation(updates.getLocation());
    }
    if (updates.getType() != null) {
        item.setType(updates.getType());
    }
    if (updates.getImageUrl() != null) {
        String newUrl = updates.getImageUrl().isBlank() ? null : updates.getImageUrl().trim();
        String previousUrl = item.getImageUrl();
        if (!Objects.equals(blankToNull(previousUrl), newUrl)) {
            firebaseStorageService.deleteReplacedImage(previousUrl, newUrl);
        }
        item.setImageUrl(newUrl);
    }
    if (updates.getVerificationQuestion() != null) {
        item.setVerificationQuestion(updates.getVerificationQuestion());
    }
    if (updates.getLatitude() != null) {
        item.setLatitude(updates.getLatitude());
    }
    if (updates.getLongitude() != null) {
        item.setLongitude(updates.getLongitude());
    }

    return itemRepository.save(item);
}
```
- **What it does:** partial update of owner-editable fields; deletes old Storage image when URL changes.
- **Where it's called:** `ItemController.updateItem`.
- **Why it's written this way:** null fields mean “leave unchanged”; image cleanup happens before save.
- **Step by step:**
  1. Load and authorize owner.
  2. Copy non-null update fields; handle image URL replacement.
  3. Save and return.

### `public Item closeReport(String id, String userId, String closureReason)`
```java
public Item closeReport(String id, String userId, String closureReason) {
    Item item = itemRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Item not found"));
    if (!userId.equals(item.getOwnerId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Not allowed to close this report");
    }
    item.setStatus("CLOSED");
    item.setClosureReason(closureReason);
    return enrichWithOwner(itemRepository.save(item));
}
```
- **What it does:** owner closes a report with a reason (manual close path; claims close elsewhere).
- **Where it's called:** `ItemController.closeReport`.
- **Why it's written this way:** records why it closed for history UI.
- **Step by step:**
  1. Load and authorize.
  2. Set CLOSED + reason; save.
  3. Enrich and return.

### `public List<Item> findMatchesForLostItem(String lostItemId, String userId)`
```java
public List<Item> findMatchesForLostItem(String lostItemId, String userId) {
    Item lostItem = itemRepository.findById(lostItemId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Item not found"));
    if (!userId.equals(lostItem.getOwnerId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Not allowed to view matches for this item");
    }
    if (!"LOST".equalsIgnoreCase(lostItem.getType())
            || !"OPEN".equalsIgnoreCase(lostItem.getStatus())) {
        return List.of();
    }

    String category = lostItem.getCategory();
    if (!StringUtils.hasText(category)) {
        return List.of();
    }

    List<Item> candidates = lostItem.getReportedAt() != null
            ? itemRepository
            .findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategoryAndReportedAtGreaterThanEqual(
                    "Found", "OPEN", category, lostItem.getReportedAt())
            : itemRepository.findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategory(
                    "Found", "OPEN", category);

    List<Item> matches = new ArrayList<>();
    for (Item candidate : candidates) {
        if (userId.equals(candidate.getOwnerId())) {
            continue;
        }
        if (lostItem.getId() != null && lostItem.getId().equals(candidate.getId())) {
            continue;
        }
        if (isPrivateTargeted(candidate)
                && !lostItem.getId().equals(candidate.getTargetLostReportId())) {
            continue;
        }
        matches.add(candidate);
    }

    matches.sort(Comparator.comparingInt(candidate ->
            -titleOverlapScore(lostItem.getTitle(), candidate.getTitle())));
    return enrichWithOwners(matches);
}
```
- **What it does:** for the owner's open lost item, returns open found candidates in the same category (respecting targeted privacy), sorted by title overlap.
- **Where it's called:** `ItemController.getMatchesForLostItem`.
- **Why it's written this way:** only the lost owner may request matches; private targeted founds for other lost reports are excluded.
- **Step by step:**
  1. Authorize owner; require open LOST with category.
  2. Load open FOUND candidates (optionally on/after lost `reportedAt`).
  3. Filter self/private mismatches; sort by title score; enrich.

### `public List<Item> searchItems(...)`
```java
public List<Item> searchItems(String category,
                              String type,
                              String query,
                              Instant from,
                              Instant to,
                              Double latitude,
                              Double longitude,
                              String viewerId) {
    List<Criteria> criteriaList = new ArrayList<>();

    criteriaList.add(Criteria.where("status").not().regex("^closed$", "i"));
    criteriaList.add(new Criteria().orOperator(
            Criteria.where("targetLostReportId").exists(false),
            Criteria.where("targetLostReportId").is(null),
            Criteria.where("targetLostReportId").is("")));
    if (StringUtils.hasText(viewerId)) {
        criteriaList.add(Criteria.where("ownerId").ne(viewerId));
    }

    if (StringUtils.hasText(category)) {
        criteriaList.add(Criteria.where("category").is(category));
    }
    if (StringUtils.hasText(type)) {
        criteriaList.add(Criteria.where("type").is(type));
    }
    if (from != null || to != null) {
        Criteria dateCriteria = Criteria.where("reportedAt");
        if (from != null) {
            dateCriteria = dateCriteria.gte(from);
        }
        if (to != null) {
            dateCriteria = dateCriteria.lte(to);
        }
        criteriaList.add(dateCriteria);
    }
    if (StringUtils.hasText(query)) {
        String pattern = ".*" + java.util.regex.Pattern.quote(query.trim()) + ".*";
        criteriaList.add(new Criteria().orOperator(
                Criteria.where("title").regex(pattern, "i"),
                Criteria.where("description").regex(pattern, "i"),
                Criteria.where("location").regex(pattern, "i")));
    }

    Query mongoQuery = new Query();
    if (!criteriaList.isEmpty()) {
        mongoQuery.addCriteria(new Criteria().andOperator(
                criteriaList.toArray(new Criteria[0])));
    }
    mongoQuery.with(Sort.by(Sort.Direction.DESC, "reportedAt"));

    List<Item> results = mongoTemplate.find(mongoQuery, Item.class);

    if (latitude != null && longitude != null) {
        results.sort(Comparator.comparingDouble(item -> distanceKm(
                latitude, longitude, item.getLatitude(), item.getLongitude())));
    }

    return enrichWithOwners(results);
}
```
- **What it does:** filtered search excluding closed, private targeted, and the viewer's own items; newest first, or nearest if coords given.
- **Where it's called:** `ItemController.searchItems`.
- **Why it's written this way:** `MongoTemplate` criteria compose optional filters; text query is regex-escaped.
- **Step by step:**
  1. Build criteria for status, non-targeted, not viewer, plus optional filters.
  2. Run query sorted by `reportedAt` desc.
  3. Optionally re-sort by distance; enrich and return.
