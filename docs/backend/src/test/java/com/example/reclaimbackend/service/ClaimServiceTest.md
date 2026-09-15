```java
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
```

What that function does: I check that a user who does not own the item cannot review the claim.
What the function is responsible for: I test ClaimService.reviewClaim's 403 path.
Where it works: I call ClaimService.reviewClaim with reviewer other-user.
Why I chose to write the code of the function the way its written: I wrote it this way because owner-1 vs other-user is enough to hit FORBIDDEN without saving anything.
More: I use Mockito when, Claim.builder, assertThrows, and HttpStatus.FORBIDDEN.

```java
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
```

What that function does: I check that approving a claim on a linked found report closes both items as owner found.
What the function is responsible for: I test ClaimService.reviewClaim plus findLinkedOrMatchingLostReport.
Where it works: I call ClaimService.reviewClaim with APPROVED and the found-item owner.
Why I chose to write the code of the function the way its written: I wrote it this way because targetLostReportId plus claimant owning the lost item is the linked path. save answers return the same object so I can assert fields.
More: I use Mockito any, thenAnswer, verify, and ClosureReasons words "owner found".

```java
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
```

What that function does: I check that the owner of an approved claim gets the claimant's contact.
What the function is responsible for: I test ClaimService.getContactForApprovedClaim.
Where it works: I call ClaimService.getContactForApprovedClaim as owner-1.
Why I chose to write the code of the function the way its written: I wrote it this way because aPPROVED plus owner requester should load the claimant through getUserEntity.
More: I use ContactResponse, User.builder, and assertEquals.
