```java
List<Item> findByOwnerId(String ownerId);
```

What that function does: I return items reported by one user.
What the function is responsible for: I load that user's reports from the items collection.
Where it works: I am called by ItemService.getItemsByOwner, UserService.deleteAccount, ClaimService.getPendingClaimsForOwner, and ClaimService.findActiveMatchingLostReport.
Why I chose to write the code of the function the way its written: I used the field name ownerId so Spring Data can build the query from the method name.
More: I return List from MongoRepository. Spring Data implements this at runtime.

```java
List<Item> findByCategory(String category);
```

What that function does: I return items in one category.
What the function is responsible for: I load items filtered by category.
Where it works: I am called by ItemService.getItemsByCategory and ItemService.notifyOwnersOfPotentialMatches.
Why I chose to write the code of the function the way its written: I wrote it this way because category is indexed, so a simple find-by-field is enough.
More: I rely on Spring Data, which maps findByCategory to a Mongo query on category.

```java
List<Item> findByType(String type);
```

What that function does: I return items of one type, Lost or Found.
What the function is responsible for: I am a type filter on items.
Where it works: I did not find callers in this backend.
Why I chose to write the code of the function the way its written: I kept the same naming as the other finders in case a later call needs all lost or all found items.
More: I rely on Spring Data, which would run this from the method name. ItemService currently uses more specific queries instead.

```java
List<Item> findByStatus(String status);
```

What that function does: I return items with one status, OPEN or CLOSED.
What the function is responsible for: I am a status filter on items.
Where it works: I did not find callers in this backend.
Why I chose to write the code of the function the way its written: I added it next to the other simple finders even though search uses MongoTemplate instead.
More: I rely on Spring Data, which would run this from the method name.

```java
List<Item> findByTargetLostReportIdIn(List<String> targetLostReportIds);
```

What that function does: I return found reports written for any of these lost-report ids.
What the function is responsible for: I load private targeted found reports for a list of lost items.
Where it works: I am called by ItemService.getTargetedFoundReportsForLostOwner.
Why I chose to write the code of the function the way its written: I wrote it this way because in lets me fetch all targeted reports for a user's open lost items in one query.
More: I rely on Spring Data, which maps the In suffix to a Mongo $in on targetLostReportId.

```java
List<Item> findByOwnerIdAndTargetLostReportId(String ownerId, String targetLostReportId);
```

What that function does: I return found reports one user filed for a specific lost report.
What the function is responsible for: I check if this viewer already filed a linked found report.
Where it works: I am called by ItemService.hasUserAlreadyInteracted. ItemServiceTest mocks me.
Why I chose to write the code of the function the way its written: I wrote it this way because both fields together answer "did this user already act on this lost item".
More: I rely on Spring Data, which ands ownerId and targetLostReportId.

```java
List<Item> findByOwnerIdAndTypeIgnoreCaseAndStatusIgnoreCase(
        String ownerId, String type, String status);
```

What that function does: I return one user's reports of a given type and status, ignoring case.
What the function is responsible for: I load that user's open lost items for matching and targeted found lists.
Where it works: I am called by ItemService.getOpenLostItemsByOwner.
Why I chose to write the code of the function the way its written: I wrote it this way because ignoreCase covers "Lost" vs "LOST" so stored casing does not hide rows.
More: I rely on Spring Data, which builds an and-query with case-insensitive type and status.

```java
List<Item> findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategoryAndReportedAtGreaterThanEqual(
        String type, String status, String category, Instant reportedAt);
```

What that function does: I return open found reports in one category, reported at or after a lost date.
What the function is responsible for: I load match candidates that appeared on or after the lost report.
Where it works: I am called by ItemService.findMatchesForLostItem when reportedAt is set. ItemServiceTest mocks me.
Why I chose to write the code of the function the way its written: I wrote it this way because greaterThanEqual keeps older found items out of the match list.
More: I take Instant for reportedAt. Spring Data maps GreaterThanEqual to $gte.

```java
List<Item> findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategory(
        String type, String status, String category);
```

What that function does: I return open found reports in one category when the lost date is missing.
What the function is responsible for: I am the fallback match query without a date floor.
Where it works: I am called by ItemService.findMatchesForLostItem when reportedAt is null.
Why I chose to write the code of the function the way its written: I still filter type, status, and category so a missing date does not return everything.
More: I rely on Spring Data, which ands the three fields. IgnoreCase is for type and status.
