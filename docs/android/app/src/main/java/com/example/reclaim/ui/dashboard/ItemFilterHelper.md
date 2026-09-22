# ItemFilterHelper.java

## Overview
This is a **helper class** — a class of static methods with no state, grouped for reuse (see `docs/GLOSSARY.md`). It takes the full list of items and returns a new list that is filtered (by category, lost/found type, and a text search) and then sorted (by distance, date, or name). `SearchFragment` uses it whenever the user changes a filter, a sort, or the search text.

## Concepts used here
- **Helper class** — static methods, private constructor, no instances. See `docs/GLOSSARY.md`.
- **Enum** — a fixed set of named choices; here `TypeFilter`, `DateSort`, `NameSort`, `LocationSort` describe the filter/sort options.
- **`Comparator`** — a Java object that says how to order two items; several are combined to sort by more than one key.
- **Haversine formula** — a math formula for the distance between two latitude/longitude points on the globe; used for "nearer/farther" sorting.

## Libraries and imports
- **Project classes**: `Item` (the data type being filtered and sorted).
- **Java standard**: `Instant` parses the report timestamp for date sorting; `ArrayList`/`List` hold results; `Comparator` builds the sort order; `Locale` makes text comparisons predictable (lowercasing and A–Z).

## Fields
This class holds no instance fields (it is stateless). It defines four **enums** used as options:
- `TypeFilter { ALL, LOST, FOUND }` — which report type to keep.
- `DateSort { RECENT, OLDEST }` — newest-first or oldest-first.
- `NameSort { NONE, A_Z, Z_A }` — alphabetical order by title.
- `LocationSort { NONE, NEARER, FARTHER }` — distance order from the user.

## Functions

### `private static double haversineKm(double lat1, double lon1, double lat2, double lon2)`
```java
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
```
- **What it does:** Returns the distance in kilometers between two points on Earth given their latitude/longitude.
- **Where it's called:** From `distanceKm` in this class.
- **Why it's written this way:** The haversine formula is the standard way to measure great-circle distance from coordinates; the constant `6371.0` is Earth's radius in km.
- **Step by step:**
  1. Convert the latitude and longitude differences to radians.
  2. Apply the haversine formula to get an intermediate value `a`, then `c`.
  3. Multiply by Earth's radius to get kilometers.

### `private static double distanceKm(double userLat, double userLng, Item item)`
```java
private static double distanceKm(
        double userLat,
        double userLng,
        Item item) {
    if (item.getLatitude() == null || item.getLongitude() == null) {
        return Double.MAX_VALUE;
    }
    return haversineKm(userLat, userLng, item.getLatitude(), item.getLongitude());
}
```
- **What it does:** Returns how far an item is from the user, or a huge number if the item has no coordinates.
- **Where it's called:** From the distance `Comparator` built in `buildComparator`.
- **Why it's written this way:** Items without coordinates return `Double.MAX_VALUE` so they sort to the end instead of crashing on a null.
- **Step by step:**
  1. If the item has no latitude or longitude, return the maximum double (sorts last).
  2. Otherwise return the haversine distance from the user to the item.

### `private static String safeTitle(Item item)`
```java
private static String safeTitle(Item item) {
    return item.getTitle() != null ? item.getTitle() : "";
}
```
- **What it does:** Returns the item's title, or an empty string if it is null.
- **Where it's called:** From the name `Comparator`s in `buildComparator`.
- **Why it's written this way:** Comparing null titles would crash, so a null becomes `""`, which sorts safely.
- **Step by step:**
  1. Return the title if present; otherwise `""`.

### `private static Instant parseInstant(String reportedAt)`
```java
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
```
- **What it does:** Turns the stored timestamp text into an `Instant`, falling back to the epoch (very old) if it is missing or unparseable.
- **Where it's called:** From the date `Comparator`s in `buildComparator`.
- **Why it's written this way:** Returning `Instant.EPOCH` for bad data keeps sorting stable and avoids exceptions from malformed dates.
- **Step by step:**
  1. If blank, return `Instant.EPOCH`.
  2. Try to parse the ISO timestamp; on any error return `Instant.EPOCH`.

### `private static Comparator<Item> buildComparator(DateSort, NameSort, LocationSort, Double userLatitude, Double userLongitude)`
```java
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
```
- **What it does:** Builds a single `Comparator` that sorts by location first (if chosen and coordinates exist), then date, then name — chaining whichever ones are active. Returns null if nothing is selected.
- **Where it's called:** From `apply`, after filtering, to decide the final order.
- **Why it's written this way:** Collecting individual comparators into a list and chaining them with `thenComparing` lets multiple sort keys combine cleanly and skips any that are not requested. Returning null means "leave the order as-is".
- **Step by step:**
  1. Start an empty list of comparators.
  2. If a location sort is chosen and the user has coordinates, add a distance comparator (reversed for "farther").
  3. Add a date comparator for RECENT (newest first) or OLDEST.
  4. Add a name comparator for A–Z or Z–A (lowercased for consistency).
  5. If the list is empty, return null.
  6. Otherwise chain them in order with `thenComparing` and return the combined comparator.

### `public static List<Item> apply(List<Item> source, String category, String searchQuery, TypeFilter, DateSort, NameSort, LocationSort, Double userLatitude, Double userLongitude)`
```java
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
```
- **What it does:** The one public entry point: filters `source` by category, type, and search text, then sorts the survivors, and returns the new list.
- **Where it's called:** From `SearchFragment.applyFilters`, which passes the current UI selections and (optionally) the user's coordinates.
- **Why it's written this way:** It builds a fresh list rather than changing the original, so the underlying data stays intact. It uses `continue` to skip non-matching items early, and combines the free-text search across title, description, and location so a query can match any of them (case-insensitive).
- **Step by step:**
  1. Make an empty result list and normalize the query (trim + lowercase).
  2. For each item: skip it if a category is set and does not match; skip it if the type filter excludes it.
  3. If there is a query, build a lowercase "haystack" from title + description + location and skip the item if it does not contain the query.
  4. Otherwise keep the item.
  5. Build the combined comparator; if it is non-null, sort the filtered list.
  6. Return the filtered (and possibly sorted) list.
