```java
private ItemFilterHelper() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep filter and sort as static methods.
Where it works: I am not called. Java uses me when someone tries `new ItemFilterHelper()`.
Why I chose to write the code of the function the way its written: I made the constructor private so SearchFragment calls apply.
More: I use a private constructor. The class is final.

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

What that function does: I copy matching items, then I sort them.
What the function is responsible for: I am the local search filter. I do not call the server.
Where it works: I am called by SearchFragment after ItemsViewModel loads the full list.
Why I chose to write the code of the function the way its written: Category, type, and text are skipped with continue. Sort is a second step so the rules stay separate.
More: I use Locale.US for lower-case match and buildComparator.

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

What that function does: I build a combined sort: distance first if asked, then date, then name.
What the function is responsible for: I turn the enum choices into one Comparator.
Where it works: I am called by apply.
Why I chose to write the code of the function the way its written: Distance needs a user pin. thenComparing keeps later keys as tie-breakers.
More: I use Comparator.comparingDouble, thenComparing, and distanceKm.

```java
private static String safeTitle(Item item) {
    return item.getTitle() != null ? item.getTitle() : "";
}
```

What that function does: I return the title, or empty if it is null.
What the function is responsible for: I keep name sort from crashing on a missing title.
Where it works: I am called by buildComparator.
Why I chose to write the code of the function the way its written: Comparator.comparing does not like null strings.
More: I use Item.getTitle.

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

What that function does: I parse the report time, or I use epoch if it is missing or broken.
What the function is responsible for: I give date sort a real Instant.
Where it works: I am called by buildComparator.
Why I chose to write the code of the function the way its written: A bad date should not crash search. Epoch puts those rows last or first with oldest.
More: I use java.time.Instant.parse.

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

What that function does: I return how far the item is, or a huge number if it has no pin.
What the function is responsible for: I feed distance sort.
Where it works: I am called by buildComparator.
Why I chose to write the code of the function the way its written: MAX_VALUE sends items without a pin to the far end.
More: I use Item.getLatitude/getLongitude and haversineKm.

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

What that function does: I compute great-circle distance in kilometers.
What the function is responsible for: I am the map-distance math for nearer/farther sort.
Where it works: I am called by distanceKm.
Why I chose to write the code of the function the way its written: Haversine is enough for nearby campus distances. 6371 km is Earth's radius.
More: I use Math.toRadians, sin, cos, atan2, and sqrt.
