package com.example.reclaimbackend.service;

import com.example.reclaimbackend.model.Claim;
import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.repository.ClaimRepository;
import com.example.reclaimbackend.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
