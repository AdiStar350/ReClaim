# ItemServiceTest.java

## Overview
This is a **unit test** class for `ItemService` using Mockito (see `docs/GLOSSARY.md`). It stubs repositories (and unused collaborators) to check two production areas: the `hasUserAlreadyInteracted` flag on `getVisibleItem`, and match listing rules in `findMatchesForLostItem` (closed lost reports return nothing; open founds in the same category on/after the lost date are kept).

## Concepts used here
- **Service** — `ItemService` under test; see `docs/GLOSSARY.md`.
- **Model / repository** — `Item` / `Claim` fixtures and mocked repos; see `docs/GLOSSARY.md`.
- **Visibility / interaction flag** — whether the viewer already claimed or filed a targeted found.
- **Unit test / Mockito** — `@InjectMocks` service with mocked dependencies.

## Libraries and imports
- **JUnit 5** (`@Test`, assertions): test methods and expectations.
- **Mockito** (`@ExtendWith`, `@Mock`, `@InjectMocks`, `when`): fake repos and helper services.
- **Java time** (`Instant`): lost/found report timestamps for match filtering.
- **Project**: `ItemService`, models, repositories, `PushNotificationService`, `FirebaseStorageService`, `MongoTemplate`.

## Fields
- `itemRepository` / `mongoTemplate` / `userRepository` / `pushNotificationService` / `firebaseStorageService` / `claimRepository` — mocks required to construct `ItemService`.
- `itemService` (`ItemService`) — real service under test.

## Functions

### `@Test void getVisibleItem_marksFoundWhenUserHasPendingClaim()`
```java
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
```
- **What it does:** checks production detail enrichment: for a FOUND item, if the viewer already has a PENDING claim, `hasUserAlreadyInteracted` is true.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** stubs the claim lookup the service uses in `hasUserAlreadyInteracted`.
- **Step by step:**
  1. Stub found item and a pending claim for the viewer.
  2. Call `getVisibleItem`.
  3. Assert the interaction flag is true.

### `@Test void getVisibleItem_marksLostWhenUserFiledLinkedFound()`
```java
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
```
- **What it does:** checks production detail enrichment: for a LOST item, if the viewer already filed a found report targeting it, `hasUserAlreadyInteracted` is true.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** stubs `findByOwnerIdAndTargetLostReportId` for the viewer.
- **Step by step:**
  1. Stub lost item and a linked found owned by the viewer.
  2. Call `getVisibleItem`.
  3. Assert the interaction flag is true.

### `@Test void getVisibleItem_leavesFlagFalseWhenViewerHasNotActed()`
```java
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
```
- **What it does:** checks the negative case: no pending claim means the interaction flag stays false.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** empty claim list proves the flag is not stuck true.
- **Step by step:**
  1. Stub found item with no pending claims for the viewer.
  2. Call `getVisibleItem`.
  3. Assert the flag is not true.

### `@Test void findMatchesForLostItem_ignoresClosedLostReports()`
```java
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
```
- **What it does:** checks production matching: a closed lost report returns an empty match list (no candidate query needed).
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** only stubs the lost item; if status is CLOSED, the service returns early.
- **Step by step:**
  1. Stub a CLOSED lost item owned by the caller.
  2. Call `findMatchesForLostItem`.
  3. Assert the list is empty.

### `@Test void findMatchesForLostItem_keepsOpenFoundInSameCategoryOnOrAfterLostDate()`
```java
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
```
- **What it does:** checks production matching: for an open lost item with a report date, open found items in the same category reported on/after that date are returned.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** stubs the dated repository query the service uses when `reportedAt` is set.
- **Step by step:**
  1. Stub an OPEN lost item with category and `reportedAt`.
  2. Stub the repository to return one OPEN found candidate after that date.
  3. Assert exactly that found id is returned.
