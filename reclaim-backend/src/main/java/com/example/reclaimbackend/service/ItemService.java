package com.example.reclaimbackend.service;

import com.example.reclaimbackend.dto.ContactResponse;
import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.model.User;
import com.example.reclaimbackend.repository.ClaimRepository;
import com.example.reclaimbackend.repository.ItemRepository;
import com.example.reclaimbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Create, find, and filter lost and found items.
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository itemRepository;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;
    private final FirebaseStorageService firebaseStorageService;
    private final ClaimRepository claimRepository;

    /**
     * Feed for home and search. Hides closed reports and the viewer's own.
     * Those stay on My Items and Report History.
     */
    public List<Item> getAllItems(String viewerId) {
        List<Item> items = itemRepository.findAll();
        items.removeIf(item -> "CLOSED".equalsIgnoreCase(item.getStatus())
                || isPrivateTargeted(item)
                || (viewerId != null && viewerId.equals(item.getOwnerId())));
        return enrichWithOwners(items);
    }

    /**
     * Finds one item by id if the viewer is allowed to see it.
     */
    public Optional<Item> getVisibleItem(String id, String viewerId) {
        return itemRepository.findById(id)
                .filter(item -> canViewerSee(item, viewerId))
                .map(this::enrichWithOwner)
                .map(item -> enrichViewerInteraction(item, viewerId));
    }

    /**
     * Finds one item by id.
     *
     * @param id the item
     * @return the item, or empty
     */
    public Optional<Item> getItemById(String id) {
        return itemRepository.findById(id).map(this::enrichWithOwner);
    }

    /**
     * Private found reports aimed at this user's open lost reports.
     */
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

    /**
     * Contact for a lost report so a finder can reach the reporter.
     * Found reports still use the claim flow.
     */
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

    /**
     * Open lost reports for this user. Closed ones are left out of matching.
     */
    public List<Item> getOpenLostItemsByOwner(String ownerId) {
        return itemRepository.findByOwnerIdAndTypeIgnoreCaseAndStatusIgnoreCase(
                ownerId, "Lost", "OPEN");
    }

    /**
     * Items reported by one user.
     *
     * @param ownerId the reporter
     * @return that user's items
     */
    public List<Item> getItemsByOwner(String ownerId) {
        List<Item> items = itemRepository.findByOwnerId(ownerId);
        items.sort(Comparator.comparing(Item::getReportedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return enrichWithOwners(items);
    }

    /**
     * Items in one category.
     *
     * @param category the category
     * @return matching items
     */
    public List<Item> getItemsByCategory(String category) {
        List<Item> items = itemRepository.findByCategory(category);
        items.removeIf(ItemService::isPrivateTargeted);
        return enrichWithOwners(items);
    }

    /**
     * Saves a new item as open and sets {@code reportedAt} if needed.
     *
     * @param item the item to save
     * @return the saved item
     */
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

    /**
     * A targeted found report must point at someone else's open lost report.
     */
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

    /**
     * Tells the lost-item owner that someone filed a private found report.
     */
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

    /**
     * Tells owners of similar open reports about this new one.
     * A failed push does not undo the new item.
     */
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

    /**
     * Removes an item by id.
     *
     * @param id the item
     */
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

    /**
     * Updates an item if the caller is the owner.
     */
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

    /**
     * Closes a report for its owner and records why.
     * Used for "owner found" / "owner not found". Claims close reports elsewhere.
     *
     * @param id            the item
     * @param userId        the owner
     * @param closureReason why it was closed
     * @return the closed item
     */
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

    /**
     * Open found items that may match a user's lost report.
     */
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

    /**
     * Search with filters. Newest first, or nearest if a location is given.
     */
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

    /**
     * Fills {@code ownerName} and {@code ownerProfilePictureUrl} for the response.
     * Not saved.
     */
    private Item enrichWithOwner(Item item) {
        if (item != null && item.getOwnerId() != null) {
            userRepository.findById(item.getOwnerId()).ifPresent(owner -> {
                item.setOwnerName(owner.getName());
                item.setOwnerProfilePictureUrl(owner.getProfilePictureUrl());
            });
        }
        return item;
    }

    /**
     * Sets whether this viewer already acted on the report.
     */
    private Item enrichViewerInteraction(Item item, String viewerId) {
        item.setHasUserAlreadyInteracted(hasUserAlreadyInteracted(item, viewerId));
        return item;
    }

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

    /**
     * Same as {@link #enrichWithOwner(Item)}, for a list. Reuses lookups.
     */
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

    private static boolean isPrivateTargeted(Item item) {
        return item != null && StringUtils.hasText(item.getTargetLostReportId());
    }

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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
