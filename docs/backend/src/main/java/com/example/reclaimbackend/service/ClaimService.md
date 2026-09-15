```java
public Claim submitClaim(ClaimRequest request, String claimantId) {

    // Check the item exists
    Item item = itemRepository.findById(request.getItemId())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Item not found with ID: " + request.getItemId()));

    // Check the item is still open
    if ("CLOSED".equalsIgnoreCase(item.getStatus())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This item has already been claimed and closed");
    }

    // Owners cannot claim their own item
    if (claimantId.equals(item.getOwnerId())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "You cannot claim your own item");
    }

    // Save the claim as pending
    Claim claim = Claim.builder()
        .itemId(request.getItemId())
        .claimantId(claimantId)
        .validationAnswer(request.getValidationAnswer())
        .status("PENDING")
        .build();

    Claim saved = claimRepository.save(claim);

    // Tell the owner about the claim.
    // A failed push does not undo the claim.
    try {
        if (pushNotificationService != null && item.getOwnerId() != null) {
            User owner = userService.getUserEntity(item.getOwnerId());
            pushNotificationService.notifyClaimPending(owner, item, saved);
        }
    } catch (Exception e) {
        log.warn("Claim notification failed for item {}: {}",
                item.getId(), e.getMessage());
    }

    return saved;
}
```

What that function does: I save a pending claim if the item is open and not the caller's own, then tell the owner.
What the function is responsible for: I am the submit-claim path.
Where it works: I am called by ClaimController.submitClaim from POST /api/claims.
Why I chose to write the code of the function the way its written: I take claimantId from the token path, not the body. try/catch keeps a failed push from undoing the save.
More: I use Claim.builder, itemRepository.findById, UserService.getUserEntity, and notifyClaimPending.

```java
@Transactional
public Claim reviewClaim(String claimId, String newStatus, String reviewerId) {

    // Load the claim
    Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Claim not found with ID: " + claimId));

    // Check it is still pending
    if (!"PENDING".equalsIgnoreCase(claim.getStatus())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This claim has already been reviewed (status: "
                        + claim.getStatus() + ")");
    }

    // Load the item
    Item item = itemRepository.findById(claim.getItemId())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Associated item not found with ID: " + claim.getItemId()));

    // Only the owner can review
    if (!reviewerId.equals(item.getOwnerId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the item owner can review claims on this item");
    }

    // Save the new status
    boolean approved = "APPROVED".equalsIgnoreCase(newStatus);
    claim.setStatus(newStatus.toUpperCase());
    claim.setClosureReason(approved
            ? ClosureReasons.CLAIM_APPROVED
            : ClosureReasons.CLAIM_DECLINED);
    claim.setDecisionAcknowledged(false);
    claimRepository.save(claim);

    // On approve: close the item and decline other pending claims
    if (approved) {

        // Close this report. If the claimant also has an open
        // matching lost report, close both as "owner found".
        // Otherwise just close this one as "claim approved".
        item.setStatus("CLOSED");
        Item matchingLostReport =
                findLinkedOrMatchingLostReport(item, claim.getClaimantId());
        if (matchingLostReport != null) {
            item.setClosureReason(ClosureReasons.OWNER_FOUND);
            matchingLostReport.setStatus("CLOSED");
            matchingLostReport.setClosureReason(ClosureReasons.OWNER_FOUND);
            itemRepository.save(matchingLostReport);
        } else {
            item.setClosureReason(ClosureReasons.CLAIM_APPROVED);
        }
        itemRepository.save(item);

        // Decline the other pending claims
        List<Claim> otherPendingClaims = claimRepository
                .findByItemIdAndStatus(claim.getItemId(), "PENDING");

        for (Claim otherClaim : otherPendingClaims) {
            // Skip the one we just approved
            if (!otherClaim.getId().equals(claimId)) {
                otherClaim.setStatus("REJECTED");
                otherClaim.setClosureReason(ClosureReasons.CLAIM_DECLINED);
                otherClaim.setDecisionAcknowledged(false);
                claimRepository.save(otherClaim);
                notifyClaimantOfDecision(otherClaim, item, false);
            }
        }
    }

    notifyClaimantOfDecision(claim, item, approved);

    return claim;
}
```

What that function does: I approve or decline a pending claim. Approve closes the item and declines the other pending claims.
What the function is responsible for: I am the review path. Only the item owner may call me.
Where it works: I am called by ClaimController.reviewClaim from PUT /api/claims/{claimId}/review. ClaimServiceTest checks me.
Why I chose to write the code of the function the way its written: I wrote it this way because @Transactional keeps the claim, item, and other claims together. Matching lost reports close as owner found when I can link them.
More: I use ClosureReasons, findByItemIdAndStatus, findLinkedOrMatchingLostReport, and notifyClaimantOfDecision.

```java
private void notifyClaimantOfDecision(Claim claim, Item item, boolean approved) {
    try {
        if (pushNotificationService == null || claim.getClaimantId() == null) {
            return;
        }
        User claimant = userService.getUserEntity(claim.getClaimantId());
        pushNotificationService.notifyClaimDecision(claimant, item, claim, approved);
    } catch (Exception e) {
        log.warn("Claim-decision notification failed for claim {}: {}",
                claim.getId(), e.getMessage());
    }
}
```

What that function does: I tell the claimant the decision. A failed push does not undo the review.
What the function is responsible for: I send CLAIM_DECISION.
Where it works: I am called by reviewClaim for the reviewed claim and for other declined claims. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because try/catch and a null check keep review from failing if FCM is down.
More: I use UserService.getUserEntity and PushNotificationService.notifyClaimDecision.

```java
private Item findLinkedOrMatchingLostReport(Item claimedItem, String claimantId) {
    if (claimedItem.getTargetLostReportId() != null
            && !claimedItem.getTargetLostReportId().isBlank()) {
        Item linked = itemRepository.findById(claimedItem.getTargetLostReportId())
                .orElse(null);
        if (linked != null
                && "LOST".equalsIgnoreCase(linked.getType())
                && "OPEN".equalsIgnoreCase(linked.getStatus())
                && claimantId != null
                && claimantId.equals(linked.getOwnerId())) {
            return linked;
        }
    }
    return findActiveMatchingLostReport(claimedItem, claimantId);
}
```

What that function does: I use the explicit lost-report link when this found report was filed with "I found this", otherwise I look for a similar open lost report.
What the function is responsible for: I pick which lost report to close with the found one on approve.
Where it works: I am called by reviewClaim. ClaimServiceTest.reviewClaim_closesLinkedLostAndFoundAsOwnerFound checks this through reviewClaim.
Why I chose to write the code of the function the way its written: I wrote it this way because the stored target id is stronger than a title guess, so I try it first.
More: I use findById and findActiveMatchingLostReport.

```java
private Item findActiveMatchingLostReport(Item claimedItem, String claimantId) {
    if (claimantId == null) {
        return null;
    }
    Item best = null;
    int bestScore = 0;
    for (Item candidate : itemRepository.findByOwnerId(claimantId)) {
        if (!"LOST".equalsIgnoreCase(candidate.getType())) {
            continue;
        }
        if (!"OPEN".equalsIgnoreCase(candidate.getStatus())) {
            continue;
        }
        if (candidate.getId() != null && candidate.getId().equals(claimedItem.getId())) {
            continue;
        }
        if (claimedItem.getCategory() != null
                && !claimedItem.getCategory().equalsIgnoreCase(candidate.getCategory())) {
            continue;
        }
        int score = titleOverlapScore(candidate.getTitle(), claimedItem.getTitle());
        if (score >= 1 && score > bestScore) {
            best = candidate;
            bestScore = score;
        }
    }
    return best;
}
```

What that function does: I look for the claimant's open lost report that looks like the same item. Best title overlap wins.
What the function is responsible for: I am the fallback match when there is no targetLostReportId.
Where it works: I am called by findLinkedOrMatchingLostReport. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because same category and at least one overlapping word keeps weak matches out. Highest score wins if several match.
More: I use findByOwnerId and titleOverlapScore.

```java
private int titleOverlapScore(String lostTitle, String foundTitle) {
    if (lostTitle == null || foundTitle == null) {
        return 0;
    }
    String foundLower = foundTitle.toLowerCase();
    int score = 0;
    for (String word : lostTitle.toLowerCase().split("\\s+")) {
        if (word.length() > 2 && foundLower.contains(word)) {
            score++;
        }
    }
    return score;
}
```

What that function does: I count how many longer words from the lost title show up in the found title.
What the function is responsible for: I score title overlap for matching lost reports.
Where it works: I am called by findActiveMatchingLostReport. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because same idea as ItemService so matching behaves the same way here.
More: I use toLowerCase, split, and contains. No extra libraries.

```java
public List<Claim> getClaimsByItem(String itemId) {
    return claimRepository.findByItemId(itemId);
}
```

What that function does: I return all claims on one item.
What the function is responsible for: I am the item-claims lookup.
Where it works: I am called by ClaimController.getClaimsByItem from GET /api/claims/item/{itemId}.
Why I chose to write the code of the function the way its written: I wrote it this way because the repository already filters by itemId, so I pass it through.
More: I use ClaimRepository.findByItemId.

```java
public List<Claim> getClaimsByClaimant(String claimantId) {
    List<Claim> claims = claimRepository.findByClaimantId(claimantId);
    for (Claim claim : claims) {
        if (claim.getItemId() != null) {
            itemRepository.findById(claim.getItemId()).ifPresent(item -> {
                claim.setItemTitle(item.getTitle());
                claim.setItemImageUrl(item.getImageUrl());
                claim.setItemType(item.getType());
            });
        }
    }
    return claims;
}
```

What that function does: I return this user's claims and fill item title, picture, and type for the list.
What the function is responsible for: I am the my-claims lookup.
Where it works: I am called by ClaimController.getMyClaims from GET /api/claims/my-claims.
Why I chose to write the code of the function the way its written: I wrote it this way because transient item fields let the client show the list without extra item calls.
More: I use findByClaimantId and itemRepository.findById.

```java
public Claim getClaimById(String claimId) {
    return claimRepository.findById(claimId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Claim not found with ID: " + claimId));
}
```

What that function does: I load one claim by id or throw 404.
What the function is responsible for: I am the shared claim lookup.
Where it works: I am called by ClaimController.getClaimById from GET /api/claims/{claimId}. acknowledgeDecision and getContactForApprovedClaim also call me.
Why I chose to write the code of the function the way its written: I wrote it this way because one 404 path keeps those callers short.
More: I use claimRepository.findById.

```java
public List<PendingClaimResponse> getPendingClaimsForOwner(String ownerId) {
    List<Item> ownedItems = itemRepository.findByOwnerId(ownerId);
    List<PendingClaimResponse> responses = new java.util.ArrayList<>();

    for (Item item : ownedItems) {
        List<Claim> pendingClaims = claimRepository.findByItemIdAndStatus(
                item.getId(), "PENDING");
        for (Claim claim : pendingClaims) {
            String claimantName = null;
            if (claim.getClaimantId() != null) {
                try {
                    claimantName = userService.getUserEntity(claim.getClaimantId()).getName();
                } catch (Exception ignored) {
                    // Name is only for the review list.
                }
            }
            responses.add(PendingClaimResponse.builder()
                    .claimId(claim.getId())
                    .itemId(item.getId())
                    .itemTitle(item.getTitle())
                    .itemImageUrl(item.getImageUrl())
                    .claimantId(claim.getClaimantId())
                    .claimantName(claimantName)
                    .validationAnswer(claim.getValidationAnswer())
                    .status(claim.getStatus())
                    .verificationQuestion(item.getVerificationQuestion())
                    .build());
        }
    }

    return responses;
}
```

What that function does: I collect pending claims on items this user reported, with names and the hidden question.
What the function is responsible for: I am the owner's review list.
Where it works: I am called by ClaimController.getPendingClaimsOnMyItems from GET /api/claims/pending-on-my-items.
Why I chose to write the code of the function the way its written: I swallow a missing claimant name so one bad user id does not empty the whole list. I include the question so the owner can compare the answer.
More: I use findByOwnerId, findByItemIdAndStatus, and PendingClaimResponse.builder.

```java
public Claim acknowledgeDecision(String claimId, String claimantId) {
    Claim claim = getClaimById(claimId);
    if (!claimantId.equals(claim.getClaimantId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the claimant can acknowledge this decision");
    }
    if ("PENDING".equalsIgnoreCase(claim.getStatus())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This claim has not been reviewed yet");
    }
    claim.setDecisionAcknowledged(true);
    return claimRepository.save(claim);
}
```

What that function does: I mark the decision as seen so the home card can go away, without changing status.
What the function is responsible for: I am the acknowledge path.
Where it works: I am called by ClaimController.acknowledgeClaimDecision from PUT /api/claims/{claimId}/acknowledge.
Why I chose to write the code of the function the way its written: I leave status and closureReason as they are. Only the claimant may call me, and only after review.
More: I use getClaimById and claimRepository.save.

```java
public ContactResponse getContactForApprovedClaim(String claimId, String requesterId) {
    Claim claim = getClaimById(claimId);

    if (!"APPROVED".equalsIgnoreCase(claim.getStatus())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Contact details are only available for approved claims");
    }

    Item item = itemRepository.findById(claim.getItemId())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Associated item not found"));

    boolean isClaimant = requesterId.equals(claim.getClaimantId());
    boolean isOwner = requesterId.equals(item.getOwnerId());
    if (!isClaimant && !isOwner) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the claimant or item owner can view contact details");
    }

    User otherParty = userService.getUserEntity(
            isClaimant ? item.getOwnerId() : claim.getClaimantId());
    return ContactResponse.builder()
            .name(otherParty.getName())
            .email(otherParty.getEmail())
            .phoneNumber(otherParty.getPhoneNumber())
            .build();
}
```

What that function does: I return the other party's contact after a claim is approved. Claimant sees the owner; owner sees the claimant.
What the function is responsible for: I am the approved-claim contact path.
Where it works: I am called by ClaimController.getClaimContact from GET /api/claims/{claimId}/contact. ClaimServiceTest.getContactForApprovedClaim_returnsClaimantToOwner checks me.
Why I chose to write the code of the function the way its written: I wrote it this way because 403 until approved, and only the two parties may see it. I pick the other user from requesterId.
More: I use getClaimById, UserService.getUserEntity, and ContactResponse.builder.
