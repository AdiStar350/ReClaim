# ItemRepository.java

## Overview
This file is a **Repository**: an interface that reads and writes one type of data in the database — here, `Item` (lost/found report) records (see `docs/GLOSSARY.md`). You declare method names following Spring Data's rules and Spring writes the queries. It also inherits standard methods (`save`, `findById`, `findAll`, `deleteById`, ...) from `MongoRepository`.

## Concepts used here
- **Repository** — an interface Spring turns into database code for one model; see `docs/GLOSSARY.md`.
- **Spring Data derived query** — queries are generated from method names.
- **`IgnoreCase` keyword** — part of a method name that makes a text match case-insensitive.
- **`In` keyword** — matches when a field equals any value in a supplied list.
- **`GreaterThanEqual` keyword** — matches when a field is at or after a given value (used for dates).

## Libraries and imports
- **Spring Data MongoDB** (`MongoRepository`): extending `MongoRepository<Item, String>` provides `save`, `findById`, `findAll`, `deleteById`, etc. `ItemService` uses `findAll`, `findById`, `save`, and `deleteById` from here in addition to the methods below.
- **Spring** (`@Repository`): marks this as a managed data-access bean.
- **Project class** (`com.example.reclaimbackend.model.Item`): the model stored.
- **Java standard** (`java.time.Instant`, `java.util.List`): `Instant` for date filtering; `List` for multi-result returns.

## Functions
> Declarations only; Spring Data generates each query from the method name.

### `List<Item> findByOwnerId(String ownerId)`
```java
List<Item> findByOwnerId(String ownerId);
```
- **What it does:** returns all items one user reported.
- **Where it's called:** `ItemService.getItemsByOwner` (the "my items" list), `ClaimService.findActiveMatchingLostReport` (scanning a claimant's reports), and `UserService.deleteAccount` (to clean up a deleted user's items).
- **Why it's written this way:** a simple name-to-query mapping on `ownerId`.
- **Step by step:** Spring matches documents where `ownerId` equals the argument and returns them as a list.

### `List<Item> findByCategory(String category)`
```java
List<Item> findByCategory(String category);
```
- **What it does:** returns all items in one category.
- **Where it's called:** `ItemService.getItemsByCategory` and `ItemService.notifyOwnersOfPotentialMatches` (to find candidates in the same category to notify).
- **Why it's written this way:** direct match on the indexed `category` field.
- **Step by step:** Spring matches on `category` and returns all matches.

### `List<Item> findByType(String type)`
```java
List<Item> findByType(String type);
```
- **What it does:** returns all items of one type (`"Lost"` or `"Found"`).
- **Where it's called:** **not currently called** anywhere in the backend. It is declared for completeness/possible future use; the active flows use the more specific `findByTypeIgnoreCase...` methods below.
- **Why it's written this way:** kept as a simple, obvious query so it's available if a plain by-type list is ever needed.
- **Step by step:** Spring would match on `type` and return all matches.

### `List<Item> findByStatus(String status)`
```java
List<Item> findByStatus(String status);
```
- **What it does:** returns all items with one status (`"OPEN"` or `"CLOSED"`).
- **Where it's called:** **not currently called** anywhere in the backend. Like `findByType`, it is declared but the live queries use the combined `...IgnoreCase...` variants.
- **Why it's written this way:** a straightforward status query kept for completeness.
- **Step by step:** Spring would match on `status` and return all matches.

### `List<Item> findByTargetLostReportIdIn(List<String> targetLostReportIds)`
```java
List<Item> findByTargetLostReportIdIn(List<String> targetLostReportIds);
```
- **What it does:** returns found reports that target any of the given lost-report ids.
- **Where it's called:** `ItemService.getTargetedFoundReportsForLostOwner`, to gather the private found reports aimed at a user's open lost reports.
- **Why it's written this way:** the `In` keyword lets one query cover many lost reports at once instead of looping.
- **Step by step:** Spring matches documents whose `targetLostReportId` is in the list and returns them.

### `List<Item> findByOwnerIdAndTargetLostReportId(String ownerId, String targetLostReportId)`
```java
List<Item> findByOwnerIdAndTargetLostReportId(String ownerId, String targetLostReportId);
```
- **What it does:** returns found reports a specific user filed against a specific lost report.
- **Where it's called:** `ItemService.hasUserAlreadyInteracted`, to check whether the viewer already filed a "found" against this lost item.
- **Why it's written this way:** answers a precise "did this user already respond?" question in one query.
- **Step by step:** Spring matches both `ownerId` and `targetLostReportId`; the caller checks whether the result is empty.

### `List<Item> findByOwnerIdAndTypeIgnoreCaseAndStatusIgnoreCase(String ownerId, String type, String status)`
```java
List<Item> findByOwnerIdAndTypeIgnoreCaseAndStatusIgnoreCase(
        String ownerId, String type, String status);
```
- **What it does:** returns one user's reports of a given type and status, ignoring letter case on type and status.
- **Where it's called:** `ItemService.getOpenLostItemsByOwner` (owner's open "Lost" reports, used for matching).
- **Why it's written this way:** `IgnoreCase` guards against stored values differing in case (e.g. `"Lost"` vs `"LOST"`, `"OPEN"` vs `"open"`).
- **Step by step:** Spring matches `ownerId` exactly and `type`/`status` case-insensitively, returning the matches.

### `List<Item> findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategoryAndReportedAtGreaterThanEqual(String type, String status, String category, Instant reportedAt)`
```java
List<Item> findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategoryAndReportedAtGreaterThanEqual(
        String type, String status, String category, Instant reportedAt);
```
- **What it does:** returns open found reports in a category that were reported at or after a given time.
- **Where it's called:** `ItemService.findMatchesForLostItem`, when the lost report has a known date (only later found reports can match it).
- **Why it's written this way:** it filters by type, status, category, and date in one query so matching stays fast and precise.
- **Step by step:** Spring matches `type`/`status` case-insensitively, `category` exactly, and `reportedAt >= argument`, then returns the matches.

### `List<Item> findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategory(String type, String status, String category)`
```java
List<Item> findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategory(
        String type, String status, String category);
```
- **What it does:** same as the previous method but without the date filter.
- **Where it's called:** `ItemService.findMatchesForLostItem`, when the lost report has no `reportedAt` date.
- **Why it's written this way:** provides a fallback when there is no date to filter on.
- **Step by step:** Spring matches `type`/`status` case-insensitively and `category` exactly, returning the matches.
