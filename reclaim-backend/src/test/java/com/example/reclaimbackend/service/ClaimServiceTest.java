package com.example.reclaimbackend.service;

import com.example.reclaimbackend.dto.ContactResponse;
import com.example.reclaimbackend.model.Claim;
import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.model.User;
import com.example.reclaimbackend.repository.ClaimRepository;
import com.example.reclaimbackend.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ClaimService claimService;

    @Test
    void reviewClaim_rejectsNonOwner() {
        Claim claim = Claim.builder()
                .id("claim-1")
                .itemId("item-1")
                .claimantId("claimant-1")
                .validationAnswer("blue lining")
                .status("PENDING")
                .build();

        Item item = new Item();
        item.setId("item-1");
        item.setOwnerId("owner-1");
        item.setStatus("OPEN");

        when(claimRepository.findById("claim-1")).thenReturn(Optional.of(claim));
        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> claimService.reviewClaim("claim-1", "APPROVED", "other-user"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void reviewClaim_closesLinkedLostAndFoundAsOwnerFound() {
        Claim claim = Claim.builder()
                .id("claim-1")
                .itemId("found-1")
                .claimantId("owner-a")
                .validationAnswer("blue lining")
                .status("PENDING")
                .build();

        Item found = new Item();
        found.setId("found-1");
        found.setOwnerId("finder-b");
        found.setStatus("OPEN");
        found.setType("Found");
        found.setTargetLostReportId("lost-1");

        Item lost = new Item();
        lost.setId("lost-1");
        lost.setOwnerId("owner-a");
        lost.setStatus("OPEN");
        lost.setType("Lost");

        when(claimRepository.findById("claim-1")).thenReturn(Optional.of(claim));
        when(itemRepository.findById("found-1")).thenReturn(Optional.of(found));
        when(itemRepository.findById("lost-1")).thenReturn(Optional.of(lost));
        when(claimRepository.findByItemIdAndStatus("found-1", "PENDING"))
                .thenReturn(List.of());
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        Claim reviewed = claimService.reviewClaim("claim-1", "APPROVED", "finder-b");

        assertEquals("APPROVED", reviewed.getStatus());
        assertEquals("CLOSED", found.getStatus());
        assertEquals("owner found", found.getClosureReason());
        assertEquals("CLOSED", lost.getStatus());
        assertEquals("owner found", lost.getClosureReason());
        verify(itemRepository).save(lost);
        verify(itemRepository).save(found);
    }

    @Test
    void getContactForApprovedClaim_returnsClaimantToOwner() {
        Claim claim = Claim.builder()
                .id("claim-1")
                .itemId("item-1")
                .claimantId("claimant-1")
                .status("APPROVED")
                .build();

        Item item = new Item();
        item.setId("item-1");
        item.setOwnerId("owner-1");

        User claimant = User.builder()
                .id("claimant-1")
                .name("Ada")
                .email("ada@example.com")
                .phoneNumber("123")
                .build();

        when(claimRepository.findById("claim-1")).thenReturn(Optional.of(claim));
        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(userService.getUserEntity("claimant-1")).thenReturn(claimant);

        ContactResponse contact = claimService.getContactForApprovedClaim("claim-1", "owner-1");

        assertEquals("Ada", contact.getName());
        assertEquals("ada@example.com", contact.getEmail());
        assertEquals("123", contact.getPhoneNumber());
    }
}
