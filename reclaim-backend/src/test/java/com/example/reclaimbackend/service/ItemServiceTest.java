package com.example.reclaimbackend.service;

import com.example.reclaimbackend.model.Claim;
import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.repository.ClaimRepository;
import com.example.reclaimbackend.repository.ItemRepository;
import com.example.reclaimbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private FirebaseStorageService firebaseStorageService;

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void getVisibleItem_marksFoundWhenUserHasPendingClaim() {
        Item found = new Item();
        found.setId("found-1");
        found.setType("Found");
        found.setOwnerId("owner-1");

        when(itemRepository.findById("found-1")).thenReturn(Optional.of(found));
        when(claimRepository.findByItemIdAndClaimantIdAndStatus(
                "found-1", "viewer-1", "PENDING"))
                .thenReturn(List.of(Claim.builder().id("claim-1").build()));

        Optional<Item> result = itemService.getVisibleItem("found-1", "viewer-1");

        assertTrue(result.isPresent());
        assertTrue(Boolean.TRUE.equals(result.get().getHasUserAlreadyInteracted()));
    }

    @Test
    void getVisibleItem_marksLostWhenUserFiledLinkedFound() {
        Item lost = new Item();
        lost.setId("lost-1");
        lost.setType("Lost");
        lost.setOwnerId("owner-1");

        Item linkedFound = new Item();
        linkedFound.setId("found-2");
        linkedFound.setTargetLostReportId("lost-1");

        when(itemRepository.findById("lost-1")).thenReturn(Optional.of(lost));
        when(itemRepository.findByOwnerIdAndTargetLostReportId("viewer-1", "lost-1"))
                .thenReturn(List.of(linkedFound));

        Optional<Item> result = itemService.getVisibleItem("lost-1", "viewer-1");

        assertTrue(result.isPresent());
        assertTrue(Boolean.TRUE.equals(result.get().getHasUserAlreadyInteracted()));
    }

    @Test
    void getVisibleItem_leavesFlagFalseWhenViewerHasNotActed() {
        Item found = new Item();
        found.setId("found-1");
        found.setType("Found");
        found.setOwnerId("owner-1");

        when(itemRepository.findById("found-1")).thenReturn(Optional.of(found));
        when(claimRepository.findByItemIdAndClaimantIdAndStatus(
                "found-1", "viewer-1", "PENDING"))
                .thenReturn(List.of());

        Optional<Item> result = itemService.getVisibleItem("found-1", "viewer-1");

        assertTrue(result.isPresent());
        assertFalse(Boolean.TRUE.equals(result.get().getHasUserAlreadyInteracted()));
    }

    @Test
    void findMatchesForLostItem_ignoresClosedLostReports() {
        Item lost = new Item();
        lost.setId("lost-1");
        lost.setType("Lost");
        lost.setStatus("CLOSED");
        lost.setOwnerId("owner-1");
        lost.setCategory("Pets");

        when(itemRepository.findById("lost-1")).thenReturn(Optional.of(lost));

        assertTrue(itemService.findMatchesForLostItem("lost-1", "owner-1").isEmpty());
    }

    @Test
    void findMatchesForLostItem_keepsOpenFoundInSameCategoryOnOrAfterLostDate() {
        Instant lostAt = Instant.parse("2026-04-01T10:00:00Z");
        Item lost = new Item();
        lost.setId("lost-1");
        lost.setType("Lost");
        lost.setStatus("OPEN");
        lost.setOwnerId("owner-1");
        lost.setCategory("Pets");
        lost.setReportedAt(lostAt);

        Item match = new Item();
        match.setId("found-1");
        match.setType("Found");
        match.setStatus("OPEN");
        match.setOwnerId("finder-1");
        match.setCategory("Pets");
        match.setReportedAt(Instant.parse("2026-04-02T10:00:00Z"));

        when(itemRepository.findById("lost-1")).thenReturn(Optional.of(lost));
        when(itemRepository
                .findByTypeIgnoreCaseAndStatusIgnoreCaseAndCategoryAndReportedAtGreaterThanEqual(
                        "Found", "OPEN", "Pets", lostAt))
                .thenReturn(List.of(match));

        List<Item> matches = itemService.findMatchesForLostItem("lost-1", "owner-1");

        assertEquals(1, matches.size());
        assertEquals("found-1", matches.get(0).getId());
    }
}
