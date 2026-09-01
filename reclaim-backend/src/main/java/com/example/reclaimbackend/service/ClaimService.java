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
 * Service layer for the Claims module.
 * <p>
 * Implements the ownership-claim business logic:
 * <ol>
 *   <li><b>Submit:</b> A user submits a claim with their verification
 *       answer. The claim is persisted with status {@code PENDING}.</li>
 *   <li><b>Review:</b> The item's original owner reviews the claim and
 *       either approves or rejects it. If approved, the item is closed
 *       and all other pending claims are auto-rejected.</li>
 * </ol>
 * </p>
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

    // ═════════════════════════════════════════════════════════════════════
    //  SUBMIT CLAIM
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Submits a new ownership claim on an item.
     * <p>
     * Validates that the referenced item exists, is still in an
     * {@code OPEN} state, and that the claimant is not the item's
     * own reporter. The claim is saved with status {@code PENDING}.
     * </p>
     *
     * @param request    the claim submission DTO (itemId + validationAnswer)
     * @param claimantId the ID of the authenticated user (from JWT)
     * @return the persisted {@link Claim} with generated ID
     * @throws ResponseStatusException 404 if the item does not exist
     * @throws ResponseStatusException 400 if the item is already closed
     * @throws ResponseStatusException 400 if the owner tries to claim their own item
     */
    public Claim submitClaim(ClaimRequest request, String claimantId) {

        // 1. Verify the item exists
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item not found with ID: " + request.getItemId()));

        // 2. Verify the item is still available
        if ("CLOSED".equalsIgnoreCase(item.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This item has already been claimed and closed");
        }

        // 3. Prevent the owner from claiming their own item
        if (claimantId.equals(item.getOwnerId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot claim your own item");
        }

        // 4. Create and persist the claim with PENDING status
        Claim claim = Claim.builder()
            .itemId(request.getItemId())
            .claimantId(claimantId)
            .validationAnswer(request.getValidationAnswer())
            .status("PENDING")
            .build();

        Claim saved = claimRepository.save(claim);

        // 5. Push a CLAIM_PENDING notification to the item's owner.
        //    Failures are logged and never block claim submission.
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

    // ═════════════════════════════════════════════════════════════════════
    //  REVIEW CLAIM (Approve / Reject)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Reviews an ownership claim (approve or reject).
     * <p>
     * <b>Authorization:</b> Only the original item owner (identified by
     * {@code ownerId}) may review claims on their items. A 403 Forbidden
     * is thrown if the requesting user is not the owner.
     * </p>
     * <p>
     * <b>Business rule — APPROVED:</b> When a claim is approved:
     * <ol>
     *   <li>The claim's status is set to {@code APPROVED}.</li>
     *   <li>The associated item's status is changed to {@code CLOSED}.</li>
     *   <li>All other {@code PENDING} claims on the same item are
     *       automatically set to {@code REJECTED}.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Business rule — REJECTED:</b> Only the individual claim's
     * status is updated; no side effects occur.
     * </p>
     *
     * @param claimId   the ID of the claim being reviewed
     * @param newStatus the new status ({@code "APPROVED"} or {@code "REJECTED"})
     * @param reviewerId the ID of the authenticated user (from JWT)
     * @return the updated {@link Claim}
     * @throws ResponseStatusException 404 if the claim does not exist
     * @throws ResponseStatusException 404 if the associated item does not exist
     * @throws ResponseStatusException 403 if the reviewer is not the item owner
     * @throws ResponseStatusException 400 if the claim is not in PENDING status
     */
    @Transactional
    public Claim reviewClaim(String claimId, String newStatus, String reviewerId) {

        // 1. Fetch the claim
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Claim not found with ID: " + claimId));

        // 2. Verify the claim is still pending
        if (!"PENDING".equalsIgnoreCase(claim.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This claim has already been reviewed (status: "
                            + claim.getStatus() + ")");
        }

        // 3. Fetch the associated item
        Item item = itemRepository.findById(claim.getItemId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Associated item not found with ID: " + claim.getItemId()));

        // 4. SECURITY: Verify the reviewer is the item's owner
        if (!reviewerId.equals(item.getOwnerId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the item owner can review claims on this item");
        }

        // 5. Update the claim status (and record its closure outcome)
        boolean approved = "APPROVED".equalsIgnoreCase(newStatus);
        claim.setStatus(newStatus.toUpperCase());
        claim.setClosureReason(approved
                ? ClosureReasons.CLAIM_APPROVED
                : ClosureReasons.CLAIM_DECLINED);
        claim.setDecisionAcknowledged(false);
        claimRepository.save(claim);

        // 6. If APPROVED → close the item + reject all other pending claims
        if (approved) {

            // 6a. Close the claimed report. If the claimant (User B) has an
            //     active matching "lost" report for the same item, close BOTH
            //     reports as "owner found"; otherwise the claimed report simply
            //     closes as "claim approved". Runs in this @Transactional method
            //     so the two updates commit atomically.
            item.setStatus("CLOSED");
            Item matchingLostReport =
                    findActiveMatchingLostReport(item, claim.getClaimantId());
            if (matchingLostReport != null) {
                item.setClosureReason(ClosureReasons.OWNER_FOUND);
                matchingLostReport.setStatus("CLOSED");
                matchingLostReport.setClosureReason(ClosureReasons.OWNER_FOUND);
                itemRepository.save(matchingLostReport);
            } else {
                item.setClosureReason(ClosureReasons.CLAIM_APPROVED);
            }
            itemRepository.save(item);

            // 6b. Reject all other PENDING claims for this item
            List<Claim> otherPendingClaims = claimRepository
                    .findByItemIdAndStatus(claim.getItemId(), "PENDING");

            for (Claim otherClaim : otherPendingClaims) {
                // Skip the one we just approved (safety check)
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
     * Pushes a {@code CLAIM_DECISION} notification to the claimant. Failures
     * are logged and never roll back the review.
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
     * Finds the claimant's active "lost" report that refers to the same item as
     * the just-approved report.
     * <p>
     * Two independent reports have no shared identifier, so "the exact same
     * item" is resolved with the app's matching heuristic: an {@code OPEN},
     * opposite-direction ("LOST") report owned by the claimant, in the same
     * category, whose title overlaps the claimed item's title. When several
     * qualify, the strongest title overlap wins.
     * </p>
     *
     * @param claimedItem the report whose claim was just approved
     * @param claimantId  the ID of the user whose claim was approved
     * @return the best-matching lost report, or {@code null} if none
     */
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
     * Counts how many significant words of {@code lostTitle} appear in
     * {@code foundTitle} (case-insensitive), mirroring the item-match heuristic.
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

    // ═════════════════════════════════════════════════════════════════════
    //  QUERY METHODS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Retrieves all claims for a specific item.
     *
     * @param itemId the item ID
     * @return list of claims on the item
     */
    public List<Claim> getClaimsByItem(String itemId) {
        return claimRepository.findByItemId(itemId);
    }

    /**
     * Retrieves all claims made by a specific user.
     *
     * @param claimantId the user ID
     * @return list of claims submitted by the user
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
     * Retrieves a single claim by its ID.
     *
     * @param claimId the claim ID
     * @return the claim
     * @throws ResponseStatusException 404 if not found
     */
    public Claim getClaimById(String claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Claim not found with ID: " + claimId));
    }

    /**
     * Returns pending claims on items owned by the given user.
     */
    public List<PendingClaimResponse> getPendingClaimsForOwner(String ownerId) {
        List<Item> ownedItems = itemRepository.findByOwnerId(ownerId);
        List<PendingClaimResponse> responses = new java.util.ArrayList<>();

        for (Item item : ownedItems) {
            List<Claim> pendingClaims = claimRepository.findByItemIdAndStatus(
                    item.getId(), "PENDING");
            for (Claim claim : pendingClaims) {
                responses.add(PendingClaimResponse.builder()
                        .claimId(claim.getId())
                        .itemId(item.getId())
                        .itemTitle(item.getTitle())
                        .claimantId(claim.getClaimantId())
                        .validationAnswer(claim.getValidationAnswer())
                        .status(claim.getStatus())
                        .verificationQuestion(item.getVerificationQuestion())
                        .build());
            }
        }

        return responses;
    }

    /**
     * Marks a reviewed claim as seen by the claimant so the home-dashboard
     * decision card is no longer shown. Does not change {@code status} or
     * {@code closureReason}.
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
     * Exposes the item owner's contact details only when the claim is approved
     * and the requester is the claimant.
     */
    public ContactResponse getContactForApprovedClaim(String claimId, String requesterId) {
        Claim claim = getClaimById(claimId);

        if (!"APPROVED".equalsIgnoreCase(claim.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Contact details are only available for approved claims");
        }

        if (!requesterId.equals(claim.getClaimantId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the claimant can view contact details");
        }

        Item item = itemRepository.findById(claim.getItemId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Associated item not found"));

        User owner = userService.getUserEntity(item.getOwnerId());
        return ContactResponse.builder()
                .name(owner.getName())
                .email(owner.getEmail())
                .phoneNumber(owner.getPhoneNumber())
                .build();
    }
}
