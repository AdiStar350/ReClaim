```java
public List<Item> getAllItems(String viewerId) {
    List<Item> items = itemRepository.findAll();
    items.removeIf(item -> "CLOSED".equalsIgnoreCase(item.getStatus())
            || isPrivateTargeted(item)
            || (viewerId != null && viewerId.equals(item.getOwnerId())));
    return enrichWithOwners(items);
}
```

What that function does: I return the home feed, hiding closed reports, private targeted founds, and the viewer's own items.
What the function is responsible for: I am the public item list for home and search-like browsing.
Where it works: I am called by ItemController.getAllItems from GET /api/items.
Why I chose to write the code of the function the way its written: I filter after findAll so My Items can still show closed and own reports on another path.
More: I use itemRepository.findAll, removeIf, isPrivateTargeted, and enrichWithOwners.

```java
public Optional<Item> getVisibleItem(String id, String viewerId) {
    return itemRepository.findById(id)
            .filter(item -> canViewerSee(item, viewerId))
            .map(this::enrichWithOwner)
            .map(item -> enrichViewerInteraction(item, viewerId));
}
```

What that function does: I load one item if this viewer may see it, and fill owner plus interaction flags.
What the function is responsible for: I am the single-item read used by the API.
Where it works: I am called by ItemController.getItemById from GET /api/items/{id}. ItemServiceTest checks the interaction flag through me.
Why I chose to write the code of the function the way its written: I wrote it this way because optional plus filter hides private targeted reports instead of throwing.
More: I use findById, canViewerSee, enrichWithOwner, and enrichViewerInteraction.

```java
public Optional<Item> getItemById(String id) {
    return itemRepository.findById(id).map(this::enrichWithOwner);
}
```

What that function does: I load one item by id and fill the owner name, with no visibility check.
What the function is responsible for: I am a simple lookup. I do not hide targeted reports.
Where it works: I did not find callers in this backend. The HTTP route uses getVisibleItem instead.
Why I chose to write the code of the function the way its written: I kept it for a plain read if an internal caller needs the item without a viewer id.
More: I use findById and enrichWithOwner.

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

What that function does: I return open found reports aimed at this user's open lost items, newest first.
What the function is responsible for: I load private targeted founds for the lost owner.
Where it works: I am called by ItemController.getTargetedFoundReports from GET /api/items/targeted-for-me.
Why I chose to write the code of the function the way its written: I collect lost ids first, then one In query, so I do not loop findById. I skip closed and non-found rows.
More: I use getOpenLostItemsByOwner, findByTargetLostReportIdIn, Comparator, and List.of.

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

What that function does: I return the reporter's contact for a lost report so a finder can reach them.
What the function is responsible for: I expose contact only for lost items, not found items.
Where it works: I am called by ItemController.getLostItemOwnerContact from GET /api/items/{id}/contact.
Why I chose to write the code of the function the way its written: I wrote it this way because found reports still use the claim flow, so I refuse non-lost types with 400.
More: I use ContactResponse.builder and userRepository.findById.

```java
public List<Item> getOpenLostItemsByOwner(String ownerId) {
    return itemRepository.findByOwnerIdAndTypeIgnoreCaseAndStatusIgnoreCase(
            ownerId, "Lost", "OPEN");
}
```

What that function does: I return this user's open lost reports.
What the function is responsible for: I load lost items used for matching and targeted-found lists. Closed ones stay out.
Where it works: I am called by getTargetedFoundReportsForLostOwner. I am unused except internally in this class.
Why I chose to write the code of the function the way its written: I wrote it this way because one repository method with IgnoreCase covers stored "Lost" vs "LOST".
More: I use ItemRepository.findByOwnerIdAndTypeIgnoreCaseAndStatusIgnoreCase.

```java
public List<Item> getItemsByOwner(String ownerId) {
    List<Item> items = itemRepository.findByOwnerId(ownerId);
    items.sort(Comparator.comparing(Item::getReportedAt,
            Comparator.nullsLast(Comparator.reverseOrder())));
    return enrichWithOwners(items);
}
```

What that function does: I return all items this user reported, newest first, including closed ones.
What the function is responsible for: I am the My Items list.
Where it works: I am called by ItemController.getMyItems from GET /api/items/my-items.
Why I chose to write the code of the function the way its written: I do not hide closed reports here because history needs them. I sort by reportedAt.
More: I use findByOwnerId, Comparator.nullsLast, and enrichWithOwners.

```java
public List<Item> getItemsByCategory(String category) {
    List<Item> items = itemRepository.findByCategory(category);
    items.removeIf(ItemService::isPrivateTargeted);
    return enrichWithOwners(items);
}
```

What that function does: I return items in one category, without private targeted founds.
What the function is responsible for: I am the category list.
Where it works: I am called by ItemController.getItemsByCategory from GET /api/items/category/{category}.
Why I chose to write the code of the function the way its written: I wrote it this way because targeted reports should stay off public category lists, same as search.
More: I use findByCategory and isPrivateTargeted.

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

What that function does: I save a new open item, set reportedAt if needed, then send the matching push.
What the function is responsible for: I create reports. Targeted founds notify the lost owner; others notify possible matches.
Where it works: I am called by ItemController.createItem from POST /api/items.
Why I chose to write the code of the function the way its written: I always start as OPEN. I notify after save so a failed push does not undo the item.
More: I use applyTargetedFoundRules, Instant.now, and the two notify helpers.

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

What that function does: I check a targeted found report points at someone else's open lost report, then force type Found.
What the function is responsible for: I validate targetLostReportId on create.
Where it works: I am called by createItem. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because blank target becomes null so public founds stay public. I copy the real lost id after trim.
More: I use Spring StringUtils.hasText and findById.

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

What that function does: I tell the lost-item owner that someone filed a private found report.
What the function is responsible for: I send TARGETED_FOUND. A failed push does not throw.
Where it works: I am called by createItem. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because try/catch so a push problem does not fail the create.
More: I use PushNotificationService.notifyTargetedFound and slf4j log.warn.

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

What that function does: I tell owners of similar open reports about this new one, at most once per owner.
What the function is responsible for: I send ITEM_MATCH pushes. A failed push does not undo the new item.
Where it works: I am called by createItem. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because hashSet stops duplicate pushes. Title overlap of at least one longer word keeps weak matches quiet.
More: I use findByCategory, titleOverlapScore, HashSet, and notifyItemMatch.

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

What that function does: I delete an item if the caller is the owner.
What the function is responsible for: I am the delete path.
Where it works: I am called by ItemController.deleteItem from DELETE /api/items/{id}.
Why I chose to write the code of the function the way its written: I wrote it this way because 403 for the wrong user is clearer than a silent no-op.
More: I use findById, deleteById, and HttpStatus.FORBIDDEN.

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

What that function does: I copy non-null update fields onto the owned item and save it.
What the function is responsible for: I am the item update. I delete the old picture when the URL changes.
Where it works: I am called by ItemController.updateItem from PUT /api/items/{id}.
Why I chose to write the code of the function the way its written: I wrote it this way because null means "leave this field". Blank image URL means remove the picture. I do not let status be patched here.
More: I use Objects.equals, blankToNull, and FirebaseStorageService.deleteReplacedImage.

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

What that function does: I close a report for its owner and store why.
What the function is responsible for: I handle owner-started closes ("owner found" / "owner not found"). Claims close reports in ClaimService instead.
Where it works: I am called by ItemController.closeReport from PUT /api/items/{id}/close.
Why I chose to write the code of the function the way its written: I trust the controller's validated reason and only check ownership here.
More: I use enrichWithOwner after save so the reply has the reporter name.

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

What that function does: I return open found items that may match this user's lost report, best title overlap first.
What the function is responsible for: I am the match list. Closed lost reports get an empty list.
Where it works: I am called by ItemController.getMatchesForLostItem from GET /api/items/{id}/matches. ItemServiceTest checks me.
Why I chose to write the code of the function the way its written: I only keep found items on or after the lost date when I have one. Targeted founds for other lost items stay out.
More: I use the two category queries, titleOverlapScore, and List.of.

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

What that function does: I count how many longer words from the lost title show up in the found title.
What the function is responsible for: I am a simple title match score.
Where it works: I am called by notifyOwnersOfPotentialMatches and findMatchesForLostItem. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because words of length 2 or less are too common, so I skip them. contains is enough without extra libraries.
More: I use toLowerCase and split on whitespace.

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

    // Search hides closed reports.
    // My Items and Report History still show them.
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

What that function does: I search items with filters, newest first, or nearest if a location is given.
What the function is responsible for: I am the search query. I hide closed reports, targeted founds, and the viewer's own items.
Where it works: I am called by ItemController.searchItems from GET /api/items/search.
Why I chose to write the code of the function the way its written: I wrote it this way because mongoTemplate lets me combine optional filters. Pattern.quote stops regex tricks in the search box. Distance sort is in memory because a geo index is not required for this.
More: I use Criteria, Query, Sort, MongoTemplate.find, Pattern.quote, and distanceKm.

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

What that function does: I fill ownerName and ownerProfilePictureUrl on one item for the reply.
What the function is responsible for: I add display fields that are not saved.
Where it works: I am called by getVisibleItem, getItemById, and closeReport. I am unused except internally in this class.
Why I chose to write the code of the function the way its written: I wrote it this way because transient fields keep Mongo clean. Missing owner is ignored.
More: I use userRepository.findById and Item setters.

```java
private Item enrichViewerInteraction(Item item, String viewerId) {
    item.setHasUserAlreadyInteracted(hasUserAlreadyInteracted(item, viewerId));
    return item;
}
```

What that function does: I set whether this viewer already acted on the report.
What the function is responsible for: I attach hasUserAlreadyInteracted for the single-item reply.
Where it works: I am called by getVisibleItem. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because a one-line wrapper keeps getVisibleItem's map chain readable.
More: I use hasUserAlreadyInteracted.

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

What that function does: I return true if this viewer already claimed a found report or filed a found report for this lost item.
What the function is responsible for: I compute the interaction flag. The owner is never marked as having interacted.
Where it works: I am called by enrichViewerInteraction. ItemServiceTest checks it through getVisibleItem.
Why I chose to write the code of the function the way its written: I wrote it this way because found vs lost need different checks. Empty list means no prior action.
More: I use ClaimRepository.findByItemIdAndClaimantIdAndStatus and ItemRepository.findByOwnerIdAndTargetLostReportId.

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

What that function does: I fill owner name and picture on a list, looking up each owner at most once.
What the function is responsible for: I am the list version of enrichWithOwner.
Where it works: I am called by getAllItems, targeted founds, my items, category, matches, and search. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because a HashMap cache avoids N lookups when many items share an owner.
More: I use HashMap and userRepository.findById.

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

What that function does: I return distance in kilometres, or a huge number if the item has no point.
What the function is responsible for: I sort search results by nearness.
Where it works: I am called by searchItems. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because items without coords go to the end via MAX_VALUE. The usual haversine formula is enough.
More: I use Math.toRadians, sin, cos, atan2, and sqrt. Earth radius 6371 km.

```java
private static boolean isPrivateTargeted(Item item) {
    return item != null && StringUtils.hasText(item.getTargetLostReportId());
}
```

What that function does: I return true if this item is a found report aimed at a specific lost report.
What the function is responsible for: I am the shared "is private" check.
Where it works: I am called by getAllItems, createItem, notifyOwnersOfPotentialMatches, category, matches, search's sibling filters, and canViewerSee. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because hasText treats blank target as public.
More: I use Spring StringUtils.hasText.

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

What that function does: I allow a private targeted found only for its reporter or the lost-item owner.
What the function is responsible for: I am the visibility rule for getVisibleItem.
Where it works: I am called by getVisibleItem. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because public items always pass. If the linked lost report is gone, I hide the targeted found.
More: I use findById on the target lost report.

```java
private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }
    return value.trim();
}
```

What that function does: I turn null or blank into null, otherwise I trim.
What the function is responsible for: I compare picture URLs on update.
Where it works: I am called by updateItem. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because same helper as UserService so blank image means no URL.
More: I use String.isBlank and trim.
