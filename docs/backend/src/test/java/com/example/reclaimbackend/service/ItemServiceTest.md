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

What that function does: I check that a found item is marked interacted when the viewer already has a pending claim.
What the function is responsible for: I test ItemService.getVisibleItem and hasUserAlreadyInteracted for found reports.
Where it works: I call ItemService.getVisibleItem.
Why I chose to write the code of the function the way its written: I return a pending claim list so the flag should be true without a real database.
More: I use Mockito when, Claim.builder, Optional.of, and assertTrue.

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

What that function does: I check that a lost item is marked interacted when the viewer already filed a linked found report.
What the function is responsible for: I test ItemService.getVisibleItem for the lost-item interaction path.
Where it works: I call ItemService.getVisibleItem.
Why I chose to write the code of the function the way its written: I mock findByOwnerIdAndTargetLostReportId with a linked found so the flag should be true.
More: I use Mockito when and List.of.

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

What that function does: I check that the interaction flag stays false when the viewer has no pending claim.
What the function is responsible for: I test ItemService.getVisibleItem's no-action path.
Where it works: I call ItemService.getVisibleItem.
Why I chose to write the code of the function the way its written: I wrote it this way because an empty claim list is the case a first-time viewer should see.
More: I use List.of() and assertFalse.

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

What that function does: I check that a closed lost report returns no matches.
What the function is responsible for: I test ItemService.findMatchesForLostItem when status is CLOSED.
Where it works: I call ItemService.findMatchesForLostItem.
Why I chose to write the code of the function the way its written: I do not stub the found-item query because the method should return before it.
More: I use Mockito when and assertTrue isEmpty.

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

What that function does: I check that an open found item in the same category on or after the lost date is kept.
What the function is responsible for: I test ItemService.findMatchesForLostItem's happy path with a date floor.
Where it works: I call ItemService.findMatchesForLostItem.
Why I chose to write the code of the function the way its written: I stub the GreaterThanEqual query with one found item the lost owner did not report.
More: I use Instant.parse, Mockito when, and assertEquals.
