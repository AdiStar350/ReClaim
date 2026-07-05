package com.example.reclaimbackend.service;

import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Optional;

/**
 * Service layer for item management operations.
 * <p>
 * Provides business logic for creating, retrieving, and filtering
 * lost &amp; found items.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Retrieves all items from the database.
     *
     * @return list of all items
     */
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    /**
     * Retrieves a single item by its ID.
     *
     * @param id the item ID
     * @return an {@link Optional} containing the item, or empty
     */
    public Optional<Item> getItemById(String id) {
        return itemRepository.findById(id);
    }

    /**
     * Retrieves all items belonging to a specific user.
     *
     * @param ownerId the owner's user ID
     * @return list of items reported by the user
     */
    public List<Item> getItemsByOwner(String ownerId) {
        return itemRepository.findByOwnerId(ownerId);
    }

    /**
     * Retrieves all items matching a given category.
     *
     * @param category the category to filter by
     * @return list of matching items
     */
    public List<Item> getItemsByCategory(String category) {
        return itemRepository.findByCategory(category);
    }

    /**
     * Creates a new item in the database.
     * <p>
     * Sets the initial status to "OPEN" and stamps {@code reportedAt}.
     * </p>
     *
     * @param item the item to create
     * @return the saved item with generated ID
     */
    public Item createItem(Item item) {
        item.setStatus("OPEN");
        if (item.getReportedAt() == null) {
            item.setReportedAt(Instant.now());
        }
        return itemRepository.save(item);
    }

    /**
     * Deletes an item by its ID.
     *
     * @param id the item ID
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
     * Updates an item owned by the authenticated user.
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
            item.setImageUrl(updates.getImageUrl());
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
     * Finds open found items that may match a user's lost item report.
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
        return matches;
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
     * Multi-parameter search with default sort by newest report first.
     * Optional proximity sort when latitude and longitude are provided.
     */
    public List<Item> searchItems(String category,
                                  String type,
                                  String query,
                                  Instant from,
                                  Instant to,
                                  Double latitude,
                                  Double longitude) {
        List<Criteria> criteriaList = new ArrayList<>();

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

        return results;
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
}
