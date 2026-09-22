# ItemMatchHelper.java

## Overview
This is a **helper class** — a class of static methods with no state, grouped for reuse (see `docs/GLOSSARY.md`). It finds "found" reports that might match a user's "lost" report (same category, reported on or after the loss, best title-word overlap first), and it also builds the small "nearby" preview list used on Home. `HomeFragment` uses it to render the match sections and the nearby row.

## Concepts used here
- **Helper class** — static methods, private constructor, no instances. See `docs/GLOSSARY.md`.
- **`Comparator`** — decides sort order; used here to rank matches by title overlap and to sort nearby items by distance.
- **Inner data class (`MatchGroup`)** — a tiny class that pairs one lost item with its list of candidate matches.
- **Haversine distance** — a formula for distance between two lat/long points, used for the nearby preview.

## Libraries and imports
- **Project classes**: `Item` (the data type).
- **Java standard**: `Instant` parses timestamps for the date rule; `ArrayList`/`List` hold results; `Comparator` ranks matches and nearby items; `Locale` makes lowercasing predictable for word matching.

## Fields
- `private static final int MAX_MATCHES_PER_LOST = 3;` — at most three suggested matches are shown per lost item, to keep the home screen short.

## Functions

### `private static Instant parseReportedAt(String reportedAt)`
```java
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
```
- **What it does:** Parses the report timestamp into an `Instant`, or returns null if it is missing or invalid.
- **Where it's called:** From `foundOnOrAfterLost`.
- **Why it's written this way:** Returning null (rather than throwing) lets the date rule treat unknown dates leniently instead of crashing.
- **Step by step:**
  1. If blank, return null.
  2. Try to parse the ISO timestamp; on any error return null.

### `private static boolean foundOnOrAfterLost(Item lostItem, Item foundItem)`
```java
private static boolean foundOnOrAfterLost(Item lostItem, Item foundItem) {
    Instant lostAt = parseReportedAt(lostItem.getReportedAt());
    Instant foundAt = parseReportedAt(foundItem.getReportedAt());
    if (lostAt == null) {
        return true;
    }
    return foundAt != null && !foundAt.isBefore(lostAt);
}
```
- **What it does:** Returns true if the found report's date is the same as, or later than, the lost report's date.
- **Where it's called:** From `findMatches`, as one of the checks a candidate must pass.
- **Why it's written this way:** Something can only be "found" at or after it was "lost", so earlier found reports are rejected. If the lost date is unknown, it is lenient and allows the match.
- **Step by step:**
  1. Parse both timestamps.
  2. If the lost date is unknown, allow the match (return true).
  3. Otherwise require the found date to exist and not be before the lost date.

### `private static int titleOverlapScore(String lostTitle, String foundTitle)`
```java
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
```
- **What it does:** Counts how many meaningful words from the lost title also appear in the found title — a rough similarity score.
- **Where it's called:** From `findMatches`, to rank the candidates so the most similar appears first.
- **Why it's written this way:** It ignores very short words (length ≤ 2, like "a" or "of") so only meaningful words count, giving a simple but useful "these look alike" ranking without a heavy search library.
- **Step by step:**
  1. If either title is null, score 0.
  2. Lowercase the found title.
  3. Split the lost title into words; for each word longer than two characters that appears in the found title, add one to the score.
  4. Return the total.

### `private static double distanceKm(double userLat, double userLng, Item item)`
```java
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
```
- **What it does:** Returns the kilometers between the user and an item, or a huge number if the item has no coordinates.
- **Where it's called:** From `nearbyPreview`, to sort items closest-first.
- **Why it's written this way:** It uses the same haversine math as elsewhere; items without coordinates return `Double.MAX_VALUE` so they sink to the end.
- **Step by step:**
  1. If the item has no coordinates, return the maximum double.
  2. Otherwise compute and return the haversine distance from the user.

### `public static List<Item> findMatches(List<Item> allItems, Item lostItem, String userId)`
```java
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
```
- **What it does:** Returns the found reports that could match one lost item, ranked by title similarity and capped at three.
- **Where it's called:** From `buildMatchGroups` in this class (once per lost item).
- **Why it's written this way:** A candidate must be a FOUND, still-OPEN report, not owned by the same user, in the same category, and dated on/after the loss. Ranking by negative overlap score puts the most similar first (sorting ascending on the negated score = descending by score). The cap keeps the home screen from overflowing.
- **Step by step:**
  1. Read the lost item's category and start an empty match list.
  2. For each candidate, skip it unless it is FOUND, OPEN, not owned by this user, same category, and found on/after the loss date.
  3. Add the survivors, then sort them by title overlap (most overlap first).
  4. Return at most `MAX_MATCHES_PER_LOST` (3) of them.

### `public static List<MatchGroup> buildMatchGroups(List<Item> allItems, List<Item> myLostItems, String userId)`
```java
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
```
- **What it does:** For each of the user's lost items, finds its matches and pairs them into a `MatchGroup`; only groups that actually have matches are returned.
- **Where it's called:** From `HomeFragment.renderMatches`, which then draws a header and a small list per group.
- **Why it's written this way:** It only considers the user's still-open lost items, and it skips empty groups so the home screen shows only lost items that have at least one suggestion.
- **Step by step:**
  1. Start an empty list of groups.
  2. For each of the user's items, skip it unless it is a LOST and OPEN report.
  3. Find its matches with `findMatches`.
  4. If there is at least one match, add a `MatchGroup(lostItem, matches)`.
  5. Return the groups.

### `public static List<Item> nearbyPreview(List<Item> items, Double latitude, Double longitude, int limit)`
```java
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
```
- **What it does:** Returns a short list of items for the home "nearby" row, sorted closest-first when the user's location is known.
- **Where it's called:** From `HomeFragment.renderNearby`, with a limit of `NEARBY_PREVIEW_LIMIT` (4).
- **Why it's written this way:** It copies the list first so the original is untouched, sorts by distance only if coordinates exist, and trims to the limit so the preview row stays small.
- **Step by step:**
  1. Copy the incoming list.
  2. If the user's latitude/longitude are known, sort the copy by distance (nearest first).
  3. If the copy is longer than `limit`, return only the first `limit` items; otherwise return the whole copy.

### `public MatchGroup(Item lostItem, List<Item> matches)` (inner class constructor)
```java
public static final class MatchGroup {
    public final Item lostItem;
    public final List<Item> matches;

    public MatchGroup(Item lostItem, List<Item> matches) {
        this.lostItem = lostItem;
        this.matches = matches;
    }
}
```
- **What it does:** `MatchGroup` is a tiny immutable data holder pairing one lost item with the found items that might match it. The constructor stores both.
- **Where it's called:** Created in `buildMatchGroups`; read in `HomeFragment.renderMatches` (as `group.lostItem` and `group.matches`).
- **Why it's written this way:** The fields are `public final` because this is a simple, read-only bundle passed straight to the UI — no getters are needed and the values never change after creation.
- **Step by step:**
  1. The constructor assigns the given lost item and match list to the two final fields.
