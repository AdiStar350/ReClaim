package com.example.reclaim.ui.dashboard;

import com.example.reclaim.model.Item;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Applies category, type, search, and sort filters to item lists.
 */
public final class ItemFilterHelper {

    public enum TypeFilter {
        ALL, LOST, FOUND
    }

    public enum DateSort {
        RECENT, OLDEST
    }

    public enum NameSort {
        NONE, A_Z, Z_A
    }

    public enum LocationSort {
        NONE, NEARER, FARTHER
    }

    private ItemFilterHelper() {
    }

    public static List<Item> apply(
            List<Item> source,
            String category,
            String searchQuery,
            TypeFilter typeFilter,
            DateSort dateSort,
            NameSort nameSort,
            LocationSort locationSort,
            Double userLatitude,
            Double userLongitude) {

        List<Item> filtered = new ArrayList<>();
        String query = searchQuery != null ? searchQuery.trim().toLowerCase(Locale.US) : "";

        for (Item item : source) {
            if (category != null && (item.getCategory() == null
                    || !category.equalsIgnoreCase(item.getCategory()))) {
                continue;
            }

            if (typeFilter == TypeFilter.LOST
                    && !"LOST".equalsIgnoreCase(item.getType())) {
                continue;
            }
            if (typeFilter == TypeFilter.FOUND
                    && !"FOUND".equalsIgnoreCase(item.getType())) {
                continue;
            }

            if (!query.isEmpty()) {
                String haystack = ((item.getTitle() != null ? item.getTitle() : "") + " "
                        + (item.getDescription() != null ? item.getDescription() : "") + " "
                        + (item.getLocation() != null ? item.getLocation() : ""))
                        .toLowerCase(Locale.US);
                if (!haystack.contains(query)) {
                    continue;
                }
            }

            filtered.add(item);
        }

        Comparator<Item> comparator = buildComparator(
                dateSort, nameSort, locationSort, userLatitude, userLongitude);
        if (comparator != null) {
            filtered.sort(comparator);
        }

        return filtered;
    }

    private static Comparator<Item> buildComparator(
            DateSort dateSort,
            NameSort nameSort,
            LocationSort locationSort,
            Double userLatitude,
            Double userLongitude) {

        List<Comparator<Item>> comparators = new ArrayList<>();

        if (locationSort != LocationSort.NONE
                && userLatitude != null
                && userLongitude != null) {
            Comparator<Item> distanceComparator = Comparator.comparingDouble(item ->
                    distanceKm(userLatitude, userLongitude, item));
            if (locationSort == LocationSort.FARTHER) {
                distanceComparator = distanceComparator.reversed();
            }
            comparators.add(distanceComparator);
        }

        if (dateSort == DateSort.RECENT) {
            comparators.add((a, b) -> parseInstant(b.getReportedAt())
                    .compareTo(parseInstant(a.getReportedAt())));
        } else if (dateSort == DateSort.OLDEST) {
            comparators.add(Comparator.comparing(a -> parseInstant(a.getReportedAt())));
        }

        if (nameSort == NameSort.A_Z) {
            comparators.add(Comparator.comparing(
                    item -> safeTitle(item).toLowerCase(Locale.US)));
        } else if (nameSort == NameSort.Z_A) {
            comparators.add(Comparator.<Item, String>comparing(
                    item -> safeTitle(item).toLowerCase(Locale.US)).reversed());
        }

        if (comparators.isEmpty()) {
            return null;
        }

        Comparator<Item> combined = comparators.get(0);
        for (int i = 1; i < comparators.size(); i++) {
            combined = combined.thenComparing(comparators.get(i));
        }
        return combined;
    }

    private static String safeTitle(Item item) {
        return item.getTitle() != null ? item.getTitle() : "";
    }

    private static Instant parseInstant(String reportedAt) {
        if (reportedAt == null || reportedAt.isEmpty()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(reportedAt);
        } catch (Exception ignored) {
            return Instant.EPOCH;
        }
    }

    private static double distanceKm(
            double userLat,
            double userLng,
            Item item) {
        if (item.getLatitude() == null || item.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        return haversineKm(userLat, userLng, item.getLatitude(), item.getLongitude());
    }

    private static double haversineKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {
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
