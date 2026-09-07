package com.example.reclaimbackend.controller;

import com.example.reclaimbackend.dto.CloseReportRequest;
import com.example.reclaimbackend.dto.ContactResponse;
import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Lost and found item endpoints.
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems(Authentication authentication) {
        String viewerId = authentication.getName();
        return ResponseEntity.ok(itemService.getAllItems(viewerId));
    }

    @GetMapping("/my-items")
    public ResponseEntity<List<Item>> getMyItems(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(itemService.getItemsByOwner(userId));
    }

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

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Item>> getItemsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(itemService.getItemsByCategory(category));
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<Item>> getMatchesForLostItem(
            @PathVariable String id,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(itemService.findMatchesForLostItem(id, userId));
    }

    @GetMapping("/{id}/contact")
    public ResponseEntity<ContactResponse> getLostItemOwnerContact(
            @PathVariable String id) {
        return ResponseEntity.ok(itemService.getLostItemOwnerContact(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable String id) {
        return itemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item,
                                           Authentication authentication) {
        String userId = authentication.getName();
        item.setOwnerId(userId);
        Item savedItem = itemService.createItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable String id,
                                           @RequestBody Item item,
                                           Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(itemService.updateItem(id, userId, item));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<Item> closeReport(@PathVariable String id,
                                            @Valid @RequestBody CloseReportRequest request,
                                            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(
                itemService.closeReport(id, userId, request.getClosureReason()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id,
                                           Authentication authentication) {
        String userId = authentication.getName();
        itemService.deleteItem(id, userId);
        return ResponseEntity.noContent().build();
    }
}
