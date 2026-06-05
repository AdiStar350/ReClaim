package com.example.reclaimbackend.service;

import com.example.reclaimbackend.dto.ClaimRequest;
import com.example.reclaimbackend.dto.ContactResponse;
import com.example.reclaimbackend.dto.PendingClaimResponse;
import com.example.reclaimbackend.model.Claim;
import com.example.reclaimbackend.model.Item;
import com.example.reclaimbackend.model.User;
import com.example.reclaimbackend.repository.ClaimRepository;
import com.example.reclaimbackend.repository.ItemRepository;
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
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;

    public ClaimService(ClaimRepository claimRepository,
                        ItemRepository itemRepository,
                        UserService userService) {
        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
        this.userService = userService;
    }

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
        Claim claim = new Claim(
                request.getItemId(),
                claimantId,
                request.getValidationAnswer(),
                "PENDING"
        );

        return claimRepository.save(claim);
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

        // 5. Update the claim status
        claim.setStatus(newStatus.toUpperCase());
        claimRepository.save(claim);

        // 6. If APPROVED → close the item + reject all other pending claims
        if ("APPROVED".equalsIgnoreCase(newStatus)) {

            // 6a. Close the item
            item.setStatus("CLOSED");
            itemRepository.save(item);

            // 6b. Reject all other PENDING claims for this item
            List<Claim> otherPendingClaims = claimRepository
                    .findByItemIdAndStatus(claim.getItemId(), "PENDING");

            for (Claim otherClaim : otherPendingClaims) {
                // Skip the one we just approved (safety check)
                if (!otherClaim.getId().equals(claimId)) {
                    otherClaim.setStatus("REJECTED");
                    claimRepository.save(otherClaim);
                }
            }
        }

        return claim;
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
        return claimRepository.findByClaimantId(claimantId);
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
                responses.add(new PendingClaimResponse(
                        claim.getId(),
                        item.getId(),
                        item.getTitle(),
                        claim.getClaimantId(),
                        claim.getValidationAnswer(),
                        claim.getStatus()));
            }
        }

        return responses;
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
        return new ContactResponse(
                owner.getName(),
                owner.getEmail(),
                owner.getPhoneNumber());
    }
}
