# ClaimService.java

## Overview
This is a **service** for ownership **claims** on items: submit, review, acknowledge, list, and share contact after approval (see `docs/GLOSSARY.md`). Approving a claim runs in a **MongoDB transaction**, closes the item, may also close a linked/matching lost report for the claimant, rejects other pending claims, and sends **FCM** decisions via `PushNotificationService`.

## Concepts used here
- **Service** — claim logic behind `ClaimController`; see `docs/GLOSSARY.md`.
- **Model / repository** — `Claim` and `Item` documents; see `docs/GLOSSARY.md`.
- **Transaction** — `@Transactional` on `reviewClaim` so related writes commit together; see `docs/GLOSSARY.md`.
- **Authorization** — only the item owner reviews; only parties on an approved claim see contact; see `docs/GLOSSARY.md`.
- **FCM** — pending/decision pushes; see `docs/GLOSSARY.md`.
- **DTO** — `ClaimRequest`, `PendingClaimResponse`, `ContactResponse`; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring** (`@Service`, `@Transactional`, `HttpStatus`, `ResponseStatusException`): service bean, transaction on review, API errors.
- **Lombok** (`@RequiredArgsConstructor`): injects repositories and helpers.
- **SLF4J**: warn when push fails without undoing the claim/review.
- **Project**: claim/item repos, `UserService`, `PushNotificationService`, models/DTOs, `ClosureReasons`.

## Fields
- `log` (`Logger`) — static logger.
- `claimRepository` / `itemRepository` — claim and item persistence.
- `userService` (`UserService`) — load users for pushes and contact.
- `pushNotificationService` (`PushNotificationService`) — claim-related pushes.

## Functions

### `private void notifyClaimantOfDecision(Claim claim, Item item, boolean approved)`
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
- **What it does:** sends a claim approved/declined push; swallows errors so review still succeeds.
- **Where it's called:** `reviewClaim` for the reviewed claim and for each auto-rejected sibling.
- **Why it's written this way:** notification is best-effort; DB changes must not roll back because FCM failed.
- **Step by step:**
  1. Return if push service or claimant id missing.
  2. Load claimant and call `notifyClaimDecision`.
  3. On any exception, log a warning.

### `private int titleOverlapScore(String lostTitle, String foundTitle)`
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
- **What it does:** counts how many longer-than-2-letter words from the lost title appear in the found title.
- **Where it's called:** `findActiveMatchingLostReport` in this class.
- **Why it's written this way:** a simple heuristic for “same item” without full-text search.
- **Step by step:**
  1. Return 0 if either title is null.
  2. Split lost title into words; count those with length > 2 contained in found title.
  3. Return the score.

### `private Item findActiveMatchingLostReport(Item claimedItem, String claimantId)`
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
- **What it does:** among the claimant's open lost reports in the same category, picks the best title-overlap match.
- **Where it's called:** `findLinkedOrMatchingLostReport` when there is no valid explicit link.
- **Why it's written this way:** when a found report is claimed/approved, the app can close the claimant's matching lost report too.
- **Step by step:**
  1. Return null if no claimant.
  2. Scan claimant's items for open LOST, same category, title score ≥ 1.
  3. Keep the highest score; return that item or null.

### `private Item findLinkedOrMatchingLostReport(Item claimedItem, String claimantId)`
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
- **What it does:** prefers an explicit `targetLostReportId` link owned by the claimant; otherwise falls back to title matching.
- **Where it's called:** `reviewClaim` when a claim is approved.
- **Why it's written this way:** targeted “I found this” reports already know which lost report to close.
- **Step by step:**
  1. If target id is set, load it and validate type/status/owner.
  2. If valid, return that linked lost item.
  3. Else call `findActiveMatchingLostReport`.

### `public Claim submitClaim(ClaimRequest request, String claimantId)`
```java
public Claim submitClaim(ClaimRequest request, String claimantId) {
    Item item = itemRepository.findById(request.getItemId())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Item not found with ID: " + request.getItemId()));

    if ("CLOSED".equalsIgnoreCase(item.getStatus())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This item has already been claimed and closed");
    }

    if (claimantId.equals(item.getOwnerId())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "You cannot claim your own item");
    }

    Claim claim = Claim.builder()
        .itemId(request.getItemId())
        .claimantId(claimantId)
        .validationAnswer(request.getValidationAnswer())
        .status("PENDING")
        .build();

    Claim saved = claimRepository.save(claim);

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
- **What it does:** creates a PENDING claim if the item is open and not owned by the claimant, then notifies the owner.
- **Where it's called:** `ClaimController.submitClaim`.
- **Why it's written this way:** business rules live here; push failure does not undo the saved claim.
- **Step by step:**
  1. Load item or 404; reject closed or self-claim.
  2. Save PENDING claim with validation answer.
  3. Best-effort notify owner; return saved claim.

### `public Claim reviewClaim(String claimId, String newStatus, String reviewerId)`
```java
@Transactional
public Claim reviewClaim(String claimId, String newStatus, String reviewerId) {
    Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Claim not found with ID: " + claimId));

    if (!"PENDING".equalsIgnoreCase(claim.getStatus())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This claim has already been reviewed (status: "
                        + claim.getStatus() + ")");
    }

    Item item = itemRepository.findById(claim.getItemId())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Associated item not found with ID: " + claim.getItemId()));

    if (!reviewerId.equals(item.getOwnerId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the item owner can review claims on this item");
    }

    boolean approved = "APPROVED".equalsIgnoreCase(newStatus);
    claim.setStatus(newStatus.toUpperCase());
    claim.setClosureReason(approved
            ? ClosureReasons.CLAIM_APPROVED
            : ClosureReasons.CLAIM_DECLINED);
    claim.setDecisionAcknowledged(false);
    claimRepository.save(claim);

    if (approved) {
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

        List<Claim> otherPendingClaims = claimRepository
                .findByItemIdAndStatus(claim.getItemId(), "PENDING");

        for (Claim otherClaim : otherPendingClaims) {
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
- **What it does:** owner approves/rejects a pending claim; on approve, closes the item (and maybe a matching lost report) and auto-rejects other pending claims, then notifies claimants.
- **Where it's called:** `ClaimController.reviewClaim`.
- **Why it's written this way:** `@Transactional` keeps claim/item updates consistent; only the owner may review.
- **Step by step:**
  1. Load claim/item; require PENDING and owner reviewer.
  2. Set claim status/reason; clear acknowledgment.
  3. If approved: close item (± matching lost), reject other pending, notify those claimants.
  4. Notify the reviewed claim's claimant; return the claim.

### `public List<Claim> getClaimsByItem(String itemId)`
```java
public List<Claim> getClaimsByItem(String itemId) {
    return claimRepository.findByItemId(itemId);
}
```
- **What it does:** returns all claims for one item.
- **Where it's called:** `ClaimController.getClaimsByItem`.
- **Why it's written this way:** thin repository pass-through.
- **Step by step:**
  1. Query by item id.
  2. Return the list.

### `public List<Claim> getClaimsByClaimant(String claimantId)`
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
- **What it does:** lists the user's claims and fills display fields from each item.
- **Where it's called:** `ClaimController.getMyClaims`.
- **Why it's written this way:** the UI needs title/image/type without a separate item fetch per row.
- **Step by step:**
  1. Load claims by claimant.
  2. For each, copy title/image/type from the item if found.
  3. Return the list.

### `public Claim getClaimById(String claimId)`
```java
public Claim getClaimById(String claimId) {
    return claimRepository.findById(claimId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Claim not found with ID: " + claimId));
}
```
- **What it does:** returns one claim or throws 404.
- **Where it's called:** `ClaimController.getClaimById`; also `acknowledgeDecision` and `getContactForApprovedClaim` in this class.
- **Why it's written this way:** shared not-found handling.
- **Step by step:**
  1. Find by id.
  2. Return or throw 404.

### `public List<PendingClaimResponse> getPendingClaimsForOwner(String ownerId)`
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
- **What it does:** builds a list of pending claims on the owner's items, with names and question/answer for the review UI.
- **Where it's called:** `ClaimController.getPendingClaimsOnMyItems`.
- **Why it's written this way:** one DTO per pending claim joins item + claim + optional claimant name.
- **Step by step:**
  1. Load owned items.
  2. For each, load PENDING claims and build `PendingClaimResponse` rows.
  3. Return the combined list.

### `public Claim acknowledgeDecision(String claimId, String claimantId)`
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
- **What it does:** marks a non-pending claim's decision as seen by the claimant.
- **Where it's called:** `ClaimController.acknowledgeClaimDecision`.
- **Why it's written this way:** lets the home UI dismiss decision cards without changing status/reason.
- **Step by step:**
  1. Load claim; require caller is claimant and status is not PENDING.
  2. Set `decisionAcknowledged` true and save.

### `public ContactResponse getContactForApprovedClaim(String claimId, String requesterId)`
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
- **What it does:** returns the other party's contact info only for APPROVED claims and only for claimant or owner.
- **Where it's called:** `ClaimController.getClaimContact`.
- **Why it's written this way:** privacy gate before exposing email/phone.
- **Step by step:**
  1. Require APPROVED claim.
  2. Load item; require requester is claimant or owner.
  3. Load the other user and return name/email/phone.
