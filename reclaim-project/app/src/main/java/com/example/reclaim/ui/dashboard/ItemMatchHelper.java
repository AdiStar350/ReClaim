package com.example.reclaim.ui.dashboard;

import com.example.reclaim.model.Item;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Looks for found items that might match a lost report.
 */
public final class ItemMatchHelper {

    private static final int MAX_MATCHES_PER_LOST = 3;

    private ItemMatchHelper() {
    }

    public static List<MatchGroup> buildMatchGroups(List<Item> allItems,
                                                    List<Item> myLostItems,
                                                    String userId) {
        List<MatchGroup> groups = new ArrayList<>();

        for (Item lostItem : myLostItems) {
            if (!"LOST".equalsIgnoreCase(lostItem.getType())
                    || !"OPEN".equalsIgnoreCase(lostItem.getStatus())) {
                continue;
            }
            List<Item> matches = findMatches(allItems, lostItem, userId);
            if (!matches.isEmpty()) {
                groups.add(new MatchGroup(lostItem, matches));
            }
        }
        return groups;
    }

    public static List<Item> findMatches(List<Item> allItems, Item lostItem, String userId) {
        List<Item> matches = new ArrayList<>();
        String category = lostItem.getCategory();

        for (Item candidate : allItems) {
            if (!"FOUND".equalsIgnoreCase(candidate.getType())) {
                continue;
            }
            if (!"OPEN".equalsIgnoreCase(candidate.getStatus())) {
                continue;
            }
            if (userId != null && userId.equals(candidate.getOwnerId())) {
                continue;
            }
            if (category == null || !category.equals(candidate.getCategory())) {
                continue;
            }
            if (!foundOnOrAfterLost(lostItem, candidate)) {
                continue;
            }
            matches.add(candidate);
        }

        matches.sort(Comparator.comparingInt(
                candidate -> -titleOverlapScore(lostItem.getTitle(), candidate.getTitle())));
        if (matches.size() > MAX_MATCHES_PER_LOST) {
            return matches.subList(0, MAX_MATCHES_PER_LOST);
        }
        return matches;
    }

    public static List<Item> nearbyPreview(List<Item> items,
                                           Double latitude,
                                           Double longitude,
                                           int limit) {
        List<Item> copy = new ArrayList<>(items);
        if (latitude != null && longitude != null) {
            copy.sort(Comparator.comparingDouble(item ->
                    distanceKm(latitude, longitude, item)));
        }
        if (copy.size() > limit) {
            return copy.subList(0, limit);
        }
        return copy;
    }

    /**
     * Found date should be the same day as the lost date, or later.
     */
    private static boolean foundOnOrAfterLost(Item lostItem, Item foundItem) {
        Instant lostAt = parseReportedAt(lostItem.getReportedAt());
        Instant foundAt = parseReportedAt(foundItem.getReportedAt());
        if (lostAt == null) {
            return true;
        }
        return foundAt != null && !foundAt.isBefore(lostAt);
    }

    private static Instant parseReportedAt(String reportedAt) {
        if (reportedAt == null || reportedAt.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(reportedAt);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int titleOverlapScore(String lostTitle, String foundTitle) {
        if (lostTitle == null || foundTitle == null) {
            return 0;
        }
        String foundLower = foundTitle.toLowerCase(Locale.US);
        int score = 0;
        for (String word : lostTitle.toLowerCase(Locale.US).split("\\s+")) {
            if (word.length() > 2 && foundLower.contains(word)) {
                score++;
            }
        }
        return score;
    }

    private static double distanceKm(double userLat, double userLng, Item item) {
        if (item.getLatitude() == null || item.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        double lat2 = item.getLatitude();
        double lon2 = item.getLongitude();
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - userLat);
        double dLon = Math.toRadians(lon2 - userLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    public static final class MatchGroup {
        public final Item lostItem;
        public final List<Item> matches;

        public MatchGroup(Item lostItem, List<Item> matches) {
            this.lostItem = lostItem;
            this.matches = matches;
        }
    }
}
