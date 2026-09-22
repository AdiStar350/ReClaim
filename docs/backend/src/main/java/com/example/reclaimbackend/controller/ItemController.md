# ItemController.java

## Overview
This is a **controller** for lost and found **items** (reports) under `/api/items` (see `docs/GLOSSARY.md`). Most routes need a JWT. The controller takes query/path parameters and the signed-in user id, then calls `ItemService` for search, CRUD, close, matches, and contact. Creating an item always sets `ownerId` from the token so the client cannot invent another owner.

## Concepts used here
- **Controller / endpoint / REST** — HTTP API for items; see `docs/GLOSSARY.md`.
- **Authentication** — user id from `Authentication.getName()`; see `docs/GLOSSARY.md`.
- **Model / entity** — request/response bodies often use `Item` directly; see `docs/GLOSSARY.md`.
- **DTO** — `CloseReportRequest`, `ContactResponse`; see `docs/GLOSSARY.md`.
- **Dependency injection** — `ItemService` is injected; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Web** (mappings, `ResponseEntity`, `@RequestParam`, `@PathVariable`, `@RequestBody`): routing and HTTP responses.
- **Spring Security** (`Authentication`): signed-in user id.
- **Spring formatting** (`@DateTimeFormat`): parses ISO date-time query params into `Instant`.
- **Jakarta Validation** (`@Valid`): validates `CloseReportRequest`.
- **Lombok** (`@RequiredArgsConstructor`): injects `itemService`.
- **Project types**: `Item`, DTOs, `ItemService`.

## Fields
- `itemService` (`ItemService`) — create/search/update/close/delete and matching logic.

## Functions

### `public ResponseEntity<List<Item>> getAllItems(Authentication authentication)`
```java
@GetMapping
public ResponseEntity<List<Item>> getAllItems(Authentication authentication) {
    String viewerId = authentication.getName();
    return ResponseEntity.ok(itemService.getAllItems(viewerId));
}
```
- **What it does:** returns items visible to the signed-in viewer (service applies visibility rules).
- **Where it's called:** by Spring MVC on `GET /api/items`.
- **Why it's written this way:** passing `viewerId` lets the service hide other users' closed/private rows appropriately.
- **Step by step:**
  1. Read viewer id from authentication.
  2. Call `itemService.getAllItems`.
  3. Return the list.

### `public ResponseEntity<List<Item>> getMyItems(Authentication authentication)`
```java
@GetMapping("/my-items")
public ResponseEntity<List<Item>> getMyItems(Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(itemService.getItemsByOwner(userId));
}
```
- **What it does:** lists items owned by the signed-in user.
- **Where it's called:** by Spring MVC on `GET /api/items/my-items`.
- **Why it's written this way:** owner comes from the token, not a path parameter.
- **Step by step:**
  1. Read user id from authentication.
  2. Call `itemService.getItemsByOwner`.
  3. Return the list.

### `public ResponseEntity<List<Item>> searchItems(...)`
```java
@GetMapping("/search")
public ResponseEntity<List<Item>> searchItems(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String query,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false) Double longitude,
        Authentication authentication) {
    return ResponseEntity.ok(itemService.searchItems(
            category, type, query, from, to, latitude, longitude,
            authentication.getName()));
}
```
- **What it does:** searches/filters items with optional category, type, text, date range, and coordinates for the viewer.
- **Where it's called:** by Spring MVC on `GET /api/items/search`.
- **Why it's written this way:** all filters are optional query params so the app can send only what the user filled in.
- **Step by step:**
  1. Bind optional query params (dates as ISO instants).
  2. Call `itemService.searchItems` with those filters plus the viewer id.
  3. Return the matching list.

### `public ResponseEntity<List<Item>> getItemsByCategory(@PathVariable String category)`
```java
@GetMapping("/category/{category}")
public ResponseEntity<List<Item>> getItemsByCategory(@PathVariable String category) {
    return ResponseEntity.ok(itemService.getItemsByCategory(category));
}
```
- **What it does:** returns items in one category.
- **Where it's called:** by Spring MVC on `GET /api/items/category/{category}`.
- **Why it's written this way:** simple category filter without requiring a viewer-specific search payload.
- **Step by step:**
  1. Call `itemService.getItemsByCategory`.
  2. Return the list.

### `public ResponseEntity<List<Item>> getTargetedFoundReports(Authentication authentication)`
```java
@GetMapping("/targeted-for-me")
public ResponseEntity<List<Item>> getTargetedFoundReports(
        Authentication authentication) {
    return ResponseEntity.ok(
            itemService.getTargetedFoundReportsForLostOwner(authentication.getName()));
}
```
- **What it does:** returns found reports aimed at the signed-in user's lost reports.
- **Where it's called:** by Spring MVC on `GET /api/items/targeted-for-me`.
- **Why it's written this way:** the lost-owner id comes from the token so users only see targets meant for them.
- **Step by step:**
  1. Read user id from authentication.
  2. Call `itemService.getTargetedFoundReportsForLostOwner`.
  3. Return the list.

### `public ResponseEntity<List<Item>> getMatchesForLostItem(@PathVariable String id, Authentication authentication)`
```java
@GetMapping("/{id}/matches")
public ResponseEntity<List<Item>> getMatchesForLostItem(
        @PathVariable String id,
        Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(itemService.findMatchesForLostItem(id, userId));
}
```
- **What it does:** finds candidate matches for one lost item the caller owns/may view.
- **Where it's called:** by Spring MVC on `GET /api/items/{id}/matches`.
- **Why it's written this way:** passes both item id and caller id so the service can enforce ownership/visibility.
- **Step by step:**
  1. Read user id from authentication.
  2. Call `itemService.findMatchesForLostItem`.
  3. Return matches.

### `public ResponseEntity<ContactResponse> getLostItemOwnerContact(@PathVariable String id)`
```java
@GetMapping("/{id}/contact")
public ResponseEntity<ContactResponse> getLostItemOwnerContact(
        @PathVariable String id) {
    return ResponseEntity.ok(itemService.getLostItemOwnerContact(id));
}
```
- **What it does:** returns contact info for the owner of a lost item when the service allows it.
- **Where it's called:** by Spring MVC on `GET /api/items/{id}/contact`.
- **Why it's written this way:** contact rules stay in the service; the controller only exposes the route.
- **Step by step:**
  1. Call `itemService.getLostItemOwnerContact`.
  2. Return `ContactResponse`.

### `public ResponseEntity<Item> getItemById(@PathVariable String id, Authentication authentication)`
```java
@GetMapping("/{id}")
public ResponseEntity<Item> getItemById(@PathVariable String id,
                                        Authentication authentication) {
    return itemService.getVisibleItem(id, authentication.getName())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```
- **What it does:** returns one item if the viewer is allowed to see it; otherwise `404`.
- **Where it's called:** by Spring MVC on `GET /api/items/{id}`.
- **Why it's written this way:** using `Optional` maps “not visible / missing” to HTTP `404` without leaking whether the id exists for unauthorized viewers beyond not-found.
- **Step by step:**
  1. Call `itemService.getVisibleItem`.
  2. If present, return `200` with the item.
  3. If empty, return `404`.

### `public ResponseEntity<Item> createItem(@RequestBody Item item, Authentication authentication)`
```java
@PostMapping
public ResponseEntity<Item> createItem(@RequestBody Item item,
                                       Authentication authentication) {
    String userId = authentication.getName();
    item.setOwnerId(userId);
    Item savedItem = itemService.createItem(item);
    return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
}
```
- **What it does:** creates a new report owned by the signed-in user.
- **Where it's called:** by Spring MVC on `POST /api/items`.
- **Why it's written this way:** overwriting `ownerId` from the token prevents forging ownership.
- **Step by step:**
  1. Set `item.ownerId` from authentication.
  2. Call `itemService.createItem`.
  3. Return `201` with the saved item.

### `public ResponseEntity<Item> updateItem(@PathVariable String id, @RequestBody Item item, Authentication authentication)`
```java
@PutMapping("/{id}")
public ResponseEntity<Item> updateItem(@PathVariable String id,
                                       @RequestBody Item item,
                                       Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(itemService.updateItem(id, userId, item));
}
```
- **What it does:** updates an item if the caller owns it (enforced in the service).
- **Where it's called:** by Spring MVC on `PUT /api/items/{id}`.
- **Why it's written this way:** passes both ids so the service can reject non-owners.
- **Step by step:**
  1. Read user id from authentication.
  2. Call `itemService.updateItem`.
  3. Return the updated item.

### `public ResponseEntity<Item> closeReport(@PathVariable String id, @Valid @RequestBody CloseReportRequest request, Authentication authentication)`
```java
@PutMapping("/{id}/close")
public ResponseEntity<Item> closeReport(@PathVariable String id,
                                        @Valid @RequestBody CloseReportRequest request,
                                        Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(
            itemService.closeReport(id, userId, request.getClosureReason()));
}
```
- **What it does:** closes a report with a reason chosen by the owner.
- **Where it's called:** by Spring MVC on `PUT /api/items/{id}/close`.
- **Why it's written this way:** closure reason is validated via DTO; ownership check stays in the service.
- **Step by step:**
  1. Validate `CloseReportRequest`.
  2. Call `itemService.closeReport` with id, user id, and reason.
  3. Return the updated item.

### `public ResponseEntity<Void> deleteItem(@PathVariable String id, Authentication authentication)`
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteItem(@PathVariable String id,
                                       Authentication authentication) {
    String userId = authentication.getName();
    itemService.deleteItem(id, userId);
    return ResponseEntity.noContent().build();
}
```
- **What it does:** deletes an item owned by the caller (and related cleanup in the service).
- **Where it's called:** by Spring MVC on `DELETE /api/items/{id}`.
- **Why it's written this way:** returns `204 No Content`, the usual success shape for deletes with no body.
- **Step by step:**
  1. Read user id from authentication.
  2. Call `itemService.deleteItem`.
  3. Return `204`.
