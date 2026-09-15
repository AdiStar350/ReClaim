```java
private ItemMatchHelper() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep match math as static methods.
Where it works: I am not called. Java uses me when someone tries `new ItemMatchHelper()`.
Why I chose to write the code of the function the way its written: I made the constructor private so HomeFragment calls the static methods.
More: I use a private constructor. The class is final.

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

What that function does: I build a group of found matches for each of my open lost reports.
What the function is responsible for: I am the home "possible matches" list builder.
Where it works: I am called by HomeFragment.
Why I chose to write the code of the function the way its written: Closed or found reports skip. Empty match lists are not added.
More: I use findMatches and MatchGroup.

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

What that function does: I pick open found items in the same category, not mine, found on or after the lost date.
What the function is responsible for: I am one lost report's match list, capped at 3.
Where it works: I am called by buildMatchGroups.
Why I chose to write the code of the function the way its written: Same category is the main filter. Title overlap only sorts. I skip my own finds.
More: I use titleOverlapScore, foundOnOrAfterLost, and MAX_MATCHES_PER_LOST = 3.

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

What that function does: I return the closest items, or the first ones if there is no user pin.
What the function is responsible for: I am the home nearby preview row.
Where it works: I am called by HomeFragment.
Why I chose to write the code of the function the way its written: I copy first so I do not sort the ViewModel list in place.
More: I use distanceKm when lat/lng exist.

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

What that function does: I return true if the find is on the same day or later than the lost report.
What the function is responsible for: I skip finds that happened before the item was lost.
Where it works: I am called by findMatches.
Why I chose to write the code of the function the way its written: A missing lost date lets the find through. A missing find date fails.
More: I use Instant.isBefore.

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

What that function does: I parse the report time, or I return null if it is missing or broken.
What the function is responsible for: I feed foundOnOrAfterLost.
Where it works: I am called by foundOnOrAfterLost.
Why I chose to write the code of the function the way its written: A bad date should not crash match building.
More: I use java.time.Instant.parse.

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

What that function does: I count how many lost-title words longer than 2 letters appear in the found title.
What the function is responsible for: I rank matches so closer titles sit first.
Where it works: I am called by findMatches for sort.
Why I chose to write the code of the function the way its written: Short words like "a" would match too often, so I skip length 2 and under.
More: I use Locale.US and String.split.

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

What that function does: I compute kilometers from the user to the item pin.
What the function is responsible for: I sort the nearby preview.
Where it works: I am called by nearbyPreview.
Why I chose to write the code of the function the way its written: No pin becomes MAX_VALUE so it sorts last. Haversine is enough for city distances.
More: I use Math.toRadians, sin, cos, atan2, and 6371 km.

```java
public MatchGroup(Item lostItem, List<Item> matches) {
    this.lostItem = lostItem;
    this.matches = matches;
}
```

What that function does: I store one lost report and its found matches.
What the function is responsible for: I am the row model HomeFragment inflates.
Where it works: I am created by buildMatchGroups and read by HomeFragment.
Why I chose to write the code of the function the way its written: Public fields are enough for a small holder.
More: I keep lostItem and matches.
