package com.example.reclaimbackend.controller;

import com.example.reclaimbackend.dto.ClaimRequest;
import com.example.reclaimbackend.dto.ClaimReviewRequest;
import com.example.reclaimbackend.dto.ContactResponse;
import com.example.reclaimbackend.dto.PendingClaimResponse;
import com.example.reclaimbackend.model.Claim;
import com.example.reclaimbackend.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Claims module.
 * <p>
 * Manages the "Ownership Claim" flow:
 * <ul>
 *   <li>{@code POST /api/claims} — Submit a new claim (any authenticated user).</li>
 *   <li>{@code PUT /api/claims/{claimId}/review} — Approve or reject a claim
 *       (item owner only).</li>
 *   <li>{@code GET /api/claims/item/{itemId}} — List all claims on an item.</li>
 *   <li>{@code GET /api/claims/my-claims} — List claims by the authenticated user.</li>
 * </ul>
 * All endpoints require a valid JWT token in the
 * {@code Authorization: Bearer <token>} header.
 * </p>
 */
@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  SUBMIT CLAIM
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Submits a new ownership claim on an item.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/claims}<br>
     * <b>Auth:</b> Required (Bearer token)<br>
     * <b>Request body:</b> {@link ClaimRequest} (itemId + validationAnswer)<br>
     * <b>Response:</b> 201 Created with the persisted claim
     * </p>
     * <p>
     * The {@code claimantId} is automatically extracted from the JWT
     * token — it is never provided by the client.
     * </p>
     *
     * @param request        the claim submission data
     * @param authentication the Spring Security authentication context
     * @return 201 Created with the new claim
     */
    @PostMapping
    public ResponseEntity<Claim> submitClaim(
            @Valid @RequestBody ClaimRequest request,
            Authentication authentication) {

        String claimantId = authentication.getName();
        Claim claim = claimService.submitClaim(request, claimantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(claim);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  REVIEW CLAIM (Approve / Reject)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Reviews a pending claim by approving or rejecting it.
     * <p>
     * <b>Endpoint:</b> {@code PUT /api/claims/{claimId}/review}<br>
     * <b>Auth:</b> Required (Bearer token) — <em>must be the item owner</em><br>
     * <b>Request body:</b> {@link ClaimReviewRequest} (status: APPROVED | REJECTED)<br>
     * <b>Response:</b> 200 OK with the updated claim
     * </p>
     * <p>
     * <b>Security:</b> The service verifies that the authenticated user
     * is the {@code ownerId} of the item associated with this claim.
     * Returns 403 Forbidden if the user is not the owner.
     * </p>
     * <p>
     * <b>Business logic — APPROVED:</b>
     * <ol>
     *   <li>Claim status → {@code APPROVED}</li>
     *   <li>Item status → {@code CLOSED}</li>
     *   <li>All other PENDING claims on the item → {@code REJECTED}</li>
     * </ol>
     * </p>
     *
     * @param claimId        the ID of the claim to review
     * @param request        the review decision (APPROVED or REJECTED)
     * @param authentication the Spring Security authentication context
     * @return 200 OK with the updated claim
     */
    @PutMapping("/{claimId}/review")
    public ResponseEntity<Claim> reviewClaim(
            @PathVariable String claimId,
            @Valid @RequestBody ClaimReviewRequest request,
            Authentication authentication) {

        String reviewerId = authentication.getName();
        Claim updatedClaim = claimService.reviewClaim(
                claimId, request.getStatus(), reviewerId);

        return ResponseEntity.ok(updatedClaim);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  QUERY ENDPOINTS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Retrieves all claims on a specific item.
     * <p>
     * <b>Endpoint:</b> {@code GET /api/claims/item/{itemId}}<br>
     * <b>Auth:</b> Required (Bearer token)
     * </p>
     *
     * @param itemId the item ID
     * @return 200 OK with the list of claims
     */
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<Claim>> getClaimsByItem(@PathVariable String itemId) {
        List<Claim> claims = claimService.getClaimsByItem(itemId);
        return ResponseEntity.ok(claims);
    }

    /**
     * Retrieves all claims submitted by the authenticated user.
     * <p>
     * <b>Endpoint:</b> {@code GET /api/claims/my-claims}<br>
     * <b>Auth:</b> Required (Bearer token)
     * </p>
     *
     * @param authentication the Spring Security authentication context
     * @return 200 OK with the user's claims
     */
    @GetMapping("/my-claims")
    public ResponseEntity<List<Claim>> getMyClaims(Authentication authentication) {
        String claimantId = authentication.getName();
        List<Claim> claims = claimService.getClaimsByClaimant(claimantId);
        return ResponseEntity.ok(claims);
    }

    /**
     * Retrieves a single claim by ID.
     * <p>
     * <b>Endpoint:</b> {@code GET /api/claims/{claimId}}<br>
     * <b>Auth:</b> Required (Bearer token)
     * </p>
     *
     * @param claimId the claim ID
     * @return 200 OK with the claim
     */
    @GetMapping("/{claimId}")
    public ResponseEntity<Claim> getClaimById(@PathVariable String claimId) {
        Claim claim = claimService.getClaimById(claimId);
        return ResponseEntity.ok(claim);
    }

    /**
     * Retrieves pending claims on items owned by the authenticated user.
     */
    @GetMapping("/pending-on-my-items")
    public ResponseEntity<List<PendingClaimResponse>> getPendingClaimsOnMyItems(
            Authentication authentication) {
        String ownerId = authentication.getName();
        return ResponseEntity.ok(claimService.getPendingClaimsForOwner(ownerId));
    }

    /**
     * Returns contact details for an approved claim (claimant only).
     */
    @GetMapping("/{claimId}/contact")
    public ResponseEntity<ContactResponse> getClaimContact(
            @PathVariable String claimId,
            Authentication authentication) {
        String requesterId = authentication.getName();
        ContactResponse contact = claimService.getContactForApprovedClaim(
                claimId, requesterId);
        return ResponseEntity.ok(contact);
    }
}
