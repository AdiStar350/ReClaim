package com.example.reclaimbackend.service;

import com.example.reclaimbackend.dto.ClaimRequest;
import com.example.reclaimbackend.dto.ContactResponse;
import com.example.reclaimbackend.dto.PendingClaimResponse;
import com.example.reclaimbackend.model.Claim;
import com.example.reclaimbackend.model.ClosureReasons;
import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.model.User;
import com.example.reclaimbackend.repository.ClaimRepository;
import com.example.reclaimbackend.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Submit and review ownership claims.
 * On approve, the item closes and other pending claims are declined.
 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ClaimService.class);

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final PushNotificationService pushNotificationService;

    // ---
    // Submit claim
    // ---

    /**
     * Saves a new claim if the item is open and not the user's own.
     *
     * @param request    item id and answer
     * @param claimantId the signed-in user
     * @return the saved claim
     * @throws ResponseStatusException 404 if the item is missing
     * @throws ResponseStatusException 400 if the item is closed
     * @throws ResponseStatusException 400 if the owner claims their own item
     */
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

    // ---
    // Review claim
    // ---

    /**
     * Approves or declines a claim. Only the item owner can do this.
     * Approve closes the item and declines the other pending claims.
     *
     * @param claimId   the claim
     * @param newStatus {@code "APPROVED"} or {@code "REJECTED"}
     * @param reviewerId the signed-in user
     * @return the updated claim
     * @throws ResponseStatusException 404 if the claim is missing
     * @throws ResponseStatusException 404 if the item is missing
     * @throws ResponseStatusException 403 if the reviewer is not the owner
     * @throws ResponseStatusException 400 if the claim is not pending
     */
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

    /**
     * Tells the claimant the decision. A failed push does not undo the review.
     */
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

    /**
     * Looks for the claimant's open lost report that looks like the same item.
     * Same category, overlapping title. Best title overlap wins if several match.
     *
     * @param claimedItem the report that was just approved
     * @param claimantId  the user whose claim was approved
     * @return the best matching lost report, or {@code null}
     */
    /**
     * Uses the explicit lost-report link when this found report was filed
     * with "I found this". Otherwise looks for a similar open lost report.
     */
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

    /**
     * How many longer words from {@code lostTitle} show up in {@code foundTitle}.
     */
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

    // ---
    // Lookups
    // ---

    /**
     * All claims on one item.
     *
     * @param itemId the item
     * @return claims on that item
     */
    public List<Claim> getClaimsByItem(String itemId) {
        return claimRepository.findByItemId(itemId);
    }

    /**
     * All claims made by one user.
     *
     * @param claimantId the user
     * @return that user's claims
     */
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

    /**
     * One claim by id.
     *
     * @param claimId the claim
     * @return the claim
     * @throws ResponseStatusException 404 if missing
     */
    public Claim getClaimById(String claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Claim not found with ID: " + claimId));
    }

    /**
     * Pending claims on items this user reported.
     */
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

    /**
     * Marks the decision as seen so the home card goes away.
     * Leaves {@code status} and {@code closureReason} as they are.
     */
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

    /**
     * Mutual contact after approval: claimant sees the owner, owner sees the claimant.
     */
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
}
