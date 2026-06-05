package com.example.reclaimbackend.controller;

import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for item management endpoints.
 * <p>
 * All endpoints under {@code /api/items} require a valid JWT token
 * in the {@code Authorization: Bearer <token>} header.
 * </p>
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Retrieves all items.
     * <p>
     * <b>Endpoint:</b> {@code GET /api/items}<br>
     * <b>Auth:</b> Required (Bearer token)
     * </p>
     *
     * @return 200 OK with a list of all items
     */
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        List<Item> items = itemService.getAllItems();
        return ResponseEntity.ok(items);
    }

    /**
     * Retrieves a single item by ID.
     * <p>
     * <b>Endpoint:</b> {@code GET /api/items/{id}}<br>
     * <b>Auth:</b> Required (Bearer token)
     * </p>
     *
     * @param id the item ID
     * @return 200 OK with the item, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable String id) {
        return itemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all items filtered by category.
     * <p>
     * <b>Endpoint:</b> {@code GET /api/items/category/{category}}<br>
     * <b>Auth:</b> Required (Bearer token)
     * </p>
     *
     * @param category the category to filter by
     * @return 200 OK with matching items
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Item>> getItemsByCategory(@PathVariable String category) {
        List<Item> items = itemService.getItemsByCategory(category);
        return ResponseEntity.ok(items);
    }

    /**
     * Retrieves all items reported by the authenticated user.
     * <p>
     * <b>Endpoint:</b> {@code GET /api/items/my-items}<br>
     * <b>Auth:</b> Required (Bearer token)
     * </p>
     *
     * @param authentication the Spring Security authentication context
     * @return 200 OK with the user's items
     */
    @GetMapping("/my-items")
    public ResponseEntity<List<Item>> getMyItems(Authentication authentication) {
        String userId = authentication.getName();
        List<Item> items = itemService.getItemsByOwner(userId);
        return ResponseEntity.ok(items);
    }

    /**
     * Creates a new item report.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/items}<br>
     * <b>Auth:</b> Required (Bearer token)<br>
     * The {@code ownerId} is automatically set from the JWT token.
     * </p>
     *
     * @param item           the item to create
     * @param authentication the Spring Security authentication context
     * @return 201 Created with the saved item
     */
    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item,
                                           Authentication authentication) {
        // Set the owner to the authenticated user
        String userId = authentication.getName();
        item.setOwnerId(userId);

        Item savedItem = itemService.createItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
    }

    /**
     * Deletes an item by ID.
     * <p>
     * <b>Endpoint:</b> {@code DELETE /api/items/{id}}<br>
     * <b>Auth:</b> Required (Bearer token)
     * </p>
     *
     * @param id the item ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Searches items using optional category, type, text query, date range,
     * and optional proximity sorting.
     */
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
            @RequestParam(required = false) Double longitude) {

        List<Item> items = itemService.searchItems(
                category, type, query, from, to, latitude, longitude);
        return ResponseEntity.ok(items);
    }
}
