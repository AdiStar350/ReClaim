# ClaimRepository.java

## Overview
This file is a **Repository**: an interface that reads and writes one type of data in the database — here, `Claim` records (see `docs/GLOSSARY.md`). You only declare method names following Spring Data's naming rules, and Spring writes the queries. It also inherits the standard save/find/delete methods from `MongoRepository`.

## Concepts used here
- **Repository** — an interface Spring turns into database code for one model; see `docs/GLOSSARY.md`.
- **Spring Data derived query** — the query is generated from the method name (e.g. `findByItemIdAndStatus`).
- **`List`** — the return type when a lookup can match many records.

## Libraries and imports
- **Spring Data MongoDB** (`MongoRepository`): extending `MongoRepository<Claim, String>` provides ready-made `save`, `findById`, `findAll`, `delete`, `deleteById`, etc. for `Claim`. `ClaimService` and `UserService` use `save`, `findById`, and `delete` from here.
- **Spring** (`@Repository`): marks this as a managed data-access bean.
- **Project class** (`com.example.reclaimbackend.model.Claim`): the model stored.
- **Java standard** (`java.util.List`): return type for multi-result queries.

## Functions
> Declarations only; Spring Data generates each query from the method name.

### `List<Claim> findByItemId(String itemId)`
```java
List<Claim> findByItemId(String itemId);
```
- **What it does:** returns every claim filed against one item.
- **Where it's called:** `ClaimService.getClaimsByItem` and `UserService.deleteAccount` (to delete an item's claims before deleting the item).
- **Why it's written this way:** the name maps directly to "where `itemId` equals" so no query code is needed.
- **Step by step:**
  1. Spring builds a query matching `itemId`.
  2. It runs it on the `claims` collection.
  3. It returns all matches as a list (empty if none).

### `List<Claim> findByClaimantId(String claimantId)`
```java
List<Claim> findByClaimantId(String claimantId);
```
- **What it does:** returns every claim one user has made.
- **Where it's called:** `ClaimService.getClaimsByClaimant` (the "my claims" list) and `UserService.deleteAccount` (to delete a user's own claims).
- **Why it's written this way:** direct name-to-query mapping on `claimantId`.
- **Step by step:**
  1. Spring builds a query matching `claimantId`.
  2. It runs it on `claims` and returns all matches.

### `List<Claim> findByItemIdAndStatus(String itemId, String status)`
```java
List<Claim> findByItemIdAndStatus(String itemId, String status);
```
- **What it does:** returns claims on one item that also have a given status (for example, all `PENDING` claims on an item).
- **Where it's called:** `ClaimService.reviewClaim` (to find the other pending claims and decline them on approval) and `ClaimService.getPendingClaimsForOwner`.
- **Why it's written this way:** two conditions joined with `And` in the name become an "equals X and equals Y" query.
- **Step by step:**
  1. Spring builds a query matching both `itemId` and `status`.
  2. It returns the matching claims.

### `List<Claim> findByItemIdAndClaimantIdAndStatus(String itemId, String claimantId, String status)`
```java
List<Claim> findByItemIdAndClaimantIdAndStatus(
        String itemId, String claimantId, String status);
```
- **What it does:** returns claims matching all three: a specific item, a specific claimant, and a specific status.
- **Where it's called:** `ItemService.hasUserAlreadyInteracted`, to check whether the current viewer already has a `PENDING` claim on a found item.
- **Why it's written this way:** it answers a very specific yes/no question ("did this viewer already claim this item?") in one query.
- **Step by step:**
  1. Spring builds a query matching `itemId`, `claimantId`, and `status`.
  2. It returns the matches; the caller just checks whether the list is empty.
