```java
List<Claim> findByItemId(String itemId);
```

What that function does: I return all claims on one item.
What the function is responsible for: I load claims by item id.
Where it works: I am called by ClaimService.getClaimsByItem and UserService.deleteAccount.
Why I chose to write the code of the function the way its written: I wrote it this way because claims store itemId, so a find-by-field is the natural query.
More: I rely on Spring Data, which implements this from the method name. I inherit save and findById from MongoRepository.

```java
List<Claim> findByClaimantId(String claimantId);
```

What that function does: I return all claims made by one user.
What the function is responsible for: I load that user's claims.
Where it works: I am called by ClaimService.getClaimsByClaimant and UserService.deleteAccount.
Why I chose to write the code of the function the way its written: I wrote it this way because claimantId is stored on each claim, so the method name matches the field.
More: I rely on Spring Data, which maps this to a Mongo query on claimantId.

```java
List<Claim> findByItemIdAndStatus(String itemId, String status);
```

What that function does: I return claims on one item with one status.
What the function is responsible for: I load pending claims on an item, or any other status if asked.
Where it works: I am called by ClaimService.reviewClaim and ClaimService.getPendingClaimsForOwner with PENDING. ClaimServiceTest mocks me.
Why I chose to write the code of the function the way its written: I wrote it this way because and lets me skip already-reviewed claims when listing or declining others.
More: I rely on Spring Data, which ands itemId and status.

```java
List<Claim> findByItemIdAndClaimantIdAndStatus(
        String itemId, String claimantId, String status);
```

What that function does: I return one person's claims on one item with one status.
What the function is responsible for: I check if this viewer already has a pending claim on a found item.
Where it works: I am called by ItemService.hasUserAlreadyInteracted. ItemServiceTest mocks me.
Why I chose to write the code of the function the way its written: I wrote it this way because three fields together answer "did this user already claim this item".
More: I rely on Spring Data, which ands itemId, claimantId, and status.
