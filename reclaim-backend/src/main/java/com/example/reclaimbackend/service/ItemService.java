package com.example.reclaimbackend.service;

import com.example.reclaimbackend.dto.ContactResponse;
import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.model.User;
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

    /**
     * Feed for home and search. Hides closed reports and the viewer's own.
     * Those stay on My Items and Report History.
     */
    public List<Item> getAllItems(String viewerId) {
        List<Item> items = itemRepository.findAll();
        items.removeIf(item -> "CLOSED".equalsIgnoreCase(item.getStatus())
                || (viewerId != null && viewerId.equals(item.getOwnerId())));
        return enrichWithOwners(items);
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
        return enrichWithOwners(itemRepository.findByCategory(category));
    }

    /**
     * Saves a new item as open and sets {@code reportedAt} if needed.
     *
     * @param item the item to save
     * @return the saved item
     */
    public Item createItem(Item item) {
        item.setStatus("OPEN");
        if (item.getReportedAt() == null) {
            item.setReportedAt(Instant.now());
        }
        Item saved = itemRepository.save(item);
        notifyOwnersOfPotentialMatches(saved);
        return saved;
    }

    /**
     * Tells owners of similar open reports about this new one.
     * A failed push does not undo the new item.
     */
    private void notifyOwnersOfPotentialMatches(Item newItem) {
        try {
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
        if (!"LOST".equalsIgnoreCase(lostItem.getType())) {
            return List.of();
        }

        String category = lostItem.getCategory();
        List<Item> candidates = itemRepository.findByCategory(category);
        List<Item> matches = new ArrayList<>();

        for (Item candidate : candidates) {
            if (!"FOUND".equalsIgnoreCase(candidate.getType())) {
                continue;
            }
            if (!"OPEN".equalsIgnoreCase(candidate.getStatus())) {
                continue;
            }
            if (userId.equals(candidate.getOwnerId())) {
                continue;
            }
            if (lostItem.getId() != null && lostItem.getId().equals(candidate.getId())) {
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
