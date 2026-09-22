# ClaimServiceTest.java

## Overview
This is a **unit test** class for `ClaimService` with Mockito mocks for repositories and `UserService` (see `docs/GLOSSARY.md`). It verifies three production behaviors: non-owners cannot review claims (`403`), approving a claim linked to a lost report closes both as “owner found,” and the item owner can read the claimant's contact after approval.

## Concepts used here
- **Service** — `ClaimService` under test; see `docs/GLOSSARY.md`.
- **Authorization** — only the item owner may review; see `docs/GLOSSARY.md`.
- **Model** — `Claim` / `Item` / `User` fixtures; see `docs/GLOSSARY.md`.
- **DTO** — `ContactResponse` for the contact test; see `docs/GLOSSARY.md`.
- **Unit test / Mockito** — stubs repos and verifies saves.

## Libraries and imports
- **JUnit 5** (`@Test`, assertions): test methods and expectations.
- **Mockito** (`@ExtendWith`, `@Mock`, `@InjectMocks`, `when`, `verify`, `any`): fake repositories and `UserService`.
- **Spring** (`HttpStatus`, `ResponseStatusException`): forbidden path.
- **Project**: `ClaimService`, models, `ContactResponse`, repositories.

## Fields
- `claimRepository` / `itemRepository` / `userService` — mocks.
- `claimService` (`ClaimService`) — real service under test (`PushNotificationService` is left null; production code null-checks before pushing).

## Functions

### `@Test void reviewClaim_rejectsNonOwner()`
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
- **What it does:** checks production review rules: a user who is not the item owner gets `403 Forbidden` when calling `reviewClaim`.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** stubs claim + item with a different owner than the reviewer id `"other-user"`.
- **Step by step:**
  1. Stub PENDING claim and OPEN item owned by `owner-1`.
  2. Call `reviewClaim` as `other-user`.
  3. Assert `FORBIDDEN`.

### `@Test void reviewClaim_closesLinkedLostAndFoundAsOwnerFound()`
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
- **What it does:** checks production approve path when the found report links to the claimant's lost report: both items close with closure reason `"owner found"`, and both are saved.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** sets `targetLostReportId` so `findLinkedOrMatchingLostReport` takes the explicit link branch.
- **Step by step:**
  1. Stub PENDING claim by lost owner on finder's targeted found item.
  2. Approve as the finder (item owner).
  3. Assert claim APPROVED, both items CLOSED with `"owner found"`, and `save` called for both.

### `@Test void getContactForApprovedClaim_returnsClaimantToOwner()`
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
- **What it does:** checks production contact sharing: after approval, the item owner requesting contact receives the claimant's name, email, and phone.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** stubs an APPROVED claim and the claimant user; requester is the owner.
- **Step by step:**
  1. Stub APPROVED claim, item, and claimant entity.
  2. Call `getContactForApprovedClaim` as the owner.
  3. Assert the contact fields match the claimant.
