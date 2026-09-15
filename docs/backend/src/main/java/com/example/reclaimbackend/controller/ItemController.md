```java
@GetMapping
public ResponseEntity<List<Item>> getAllItems(Authentication authentication) {
    String viewerId = authentication.getName();
    return ResponseEntity.ok(itemService.getAllItems(viewerId));
}
```

What that function does: I return the home feed of items for this viewer.
What the function is responsible for: I am GET /api/items.
Where it works: I am an HTTP endpoint. I call ItemService.getAllItems.
Why I chose to write the code of the function the way its written: I pass the viewer id so the service can hide that user's own reports and closed ones.
More: I use Authentication.getName and ResponseEntity.ok with List<Item>.

```java
@GetMapping("/my-items")
public ResponseEntity<List<Item>> getMyItems(Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(itemService.getItemsByOwner(userId));
}
```

What that function does: I return items this user reported.
What the function is responsible for: I am GET /api/items/my-items.
Where it works: I am an HTTP endpoint. I call ItemService.getItemsByOwner.
Why I chose to write the code of the function the way its written: I take the user id from the token so the caller cannot list someone else's items.
More: I use Authentication.getName.

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

What that function does: I search items with optional filters and pass the viewer id.
What the function is responsible for: I am GET /api/items/search.
Where it works: I am an HTTP endpoint. I call ItemService.searchItems.
Why I chose to write the code of the function the way its written: I wrote it this way because all query params are optional so a client can send only what it has. I parse dates as ISO instants.
More: I use @RequestParam, @DateTimeFormat ISO.DATE_TIME, Instant, and Double for map search.

```java
@GetMapping("/category/{category}")
public ResponseEntity<List<Item>> getItemsByCategory(@PathVariable String category) {
    return ResponseEntity.ok(itemService.getItemsByCategory(category));
}
```

What that function does: I return items in one category.
What the function is responsible for: I am GET /api/items/category/{category}.
Where it works: I am an HTTP endpoint. I call ItemService.getItemsByCategory.
Why I chose to write the code of the function the way its written: I keep this route thin and let the service hide private targeted reports.
More: I use @PathVariable String category.

```java
@GetMapping("/targeted-for-me")
public ResponseEntity<List<Item>> getTargetedFoundReports(
        Authentication authentication) {
    return ResponseEntity.ok(
            itemService.getTargetedFoundReportsForLostOwner(authentication.getName()));
}
```

What that function does: I return private found reports aimed at this user's open lost items.
What the function is responsible for: I am GET /api/items/targeted-for-me.
Where it works: I am an HTTP endpoint. I call ItemService.getTargetedFoundReportsForLostOwner.
Why I chose to write the code of the function the way its written: I pass only the signed-in user id so other people's targeted reports stay hidden.
More: I use Authentication.getName.

```java
@GetMapping("/{id}/matches")
public ResponseEntity<List<Item>> getMatchesForLostItem(
        @PathVariable String id,
        Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(itemService.findMatchesForLostItem(id, userId));
}
```

What that function does: I return open found items that may match this lost report.
What the function is responsible for: I am GET /api/items/{id}/matches.
Where it works: I am an HTTP endpoint. I call ItemService.findMatchesForLostItem. ItemServiceTest checks that service method.
Why I chose to write the code of the function the way its written: I pass both ids so the service can refuse a viewer who does not own the lost item.
More: I use @PathVariable id and Authentication.getName.

```java
@GetMapping("/{id}/contact")
public ResponseEntity<ContactResponse> getLostItemOwnerContact(
        @PathVariable String id) {
    return ResponseEntity.ok(itemService.getLostItemOwnerContact(id));
}
```

What that function does: I return the reporter's contact for a lost item.
What the function is responsible for: I am GET /api/items/{id}/contact.
Where it works: I am an HTTP endpoint. I call ItemService.getLostItemOwnerContact.
Why I chose to write the code of the function the way its written: I keep this route on the item, not the claim, because a finder of a lost report needs contact without claiming.
More: I return ContactResponse. This route still needs a signed-in user via SecurityConfig.

```java
@GetMapping("/{id}")
public ResponseEntity<Item> getItemById(@PathVariable String id,
                                        Authentication authentication) {
    return itemService.getVisibleItem(id, authentication.getName())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

What that function does: I return one item if this viewer may see it, or 404.
What the function is responsible for: I am GET /api/items/{id}.
Where it works: I am an HTTP endpoint. I call ItemService.getVisibleItem, not ItemService.getItemById.
Why I chose to write the code of the function the way its written: I wrote it this way because optional to 404 hides private targeted reports from the wrong viewer instead of leaking them.
More: I use Optional.map and ResponseEntity.notFound.

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

What that function does: I save a new item as belonging to the signed-in user.
What the function is responsible for: I am POST /api/items.
Where it works: I am an HTTP endpoint. I call ItemService.createItem. ItemControllerSecurityTest posts here without a token.
Why I chose to write the code of the function the way its written: I set ownerId from the token so the body cannot claim another owner. 201 marks a new report.
More: I use Item.setOwnerId, HttpStatus.CREATED, and Authentication.getName.

```java
@PutMapping("/{id}")
public ResponseEntity<Item> updateItem(@PathVariable String id,
                                       @RequestBody Item item,
                                       Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(itemService.updateItem(id, userId, item));
}
```

What that function does: I update an item if the caller is the owner.
What the function is responsible for: I am PUT /api/items/{id}.
Where it works: I am an HTTP endpoint. I call ItemService.updateItem.
Why I chose to write the code of the function the way its written: I pass the token user id so the service can refuse other users.
More: I use @PathVariable, @RequestBody Item, and Authentication.

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

What that function does: I close a report for its owner with the given reason.
What the function is responsible for: I am PUT /api/items/{id}/close.
Where it works: I am an HTTP endpoint. I call ItemService.closeReport.
Why I chose to write the code of the function the way its written: I take closureReason from a validated request so only "owner found" or "owner not found" get through.
More: I use @Valid CloseReportRequest.getClosureReason.

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteItem(@PathVariable String id,
                                       Authentication authentication) {
    String userId = authentication.getName();
    itemService.deleteItem(id, userId);
    return ResponseEntity.noContent().build();
}
```

What that function does: I delete an item if the caller is the owner.
What the function is responsible for: I am DELETE /api/items/{id}.
Where it works: I am an HTTP endpoint. I call ItemService.deleteItem.
Why I chose to write the code of the function the way its written: I wrote it this way because 204 is enough after a successful delete. The service throws if the user does not own it.
More: I use ResponseEntity.noContent and Authentication.getName.
