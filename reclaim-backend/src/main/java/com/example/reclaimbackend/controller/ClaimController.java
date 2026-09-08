package com.example.reclaimbackend.controller;

import com.example.reclaimbackend.dto.ClaimRequest;
import com.example.reclaimbackend.dto.ClaimReviewRequest;
import com.example.reclaimbackend.dto.ContactResponse;
import com.example.reclaimbackend.dto.PendingClaimResponse;
import com.example.reclaimbackend.model.Claim;
import com.example.reclaimbackend.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Submit, review, and list ownership claims.
 * All routes need a signed-in user.
 */
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    // ---
    // Submit claim
    // ---

    /**
     * Saves a new claim. The claimant comes from the token, not the body.
     *
     * @param request        item id and answer
     * @param authentication the signed-in user
     * @return 201 with the new claim
     */
    @PostMapping
    public ResponseEntity<Claim> submitClaim(
            @Valid @RequestBody ClaimRequest request,
            Authentication authentication) {

        String claimantId = authentication.getName();
        Claim claim = claimService.submitClaim(request, claimantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(claim);
    }

    // ---
    // Review claim
    // ---

    /**
     * Approves or declines a claim. Only the item owner can do this.
     *
     * @param claimId        the claim
     * @param request        APPROVED or REJECTED
     * @param authentication the signed-in user
     * @return 200 with the updated claim
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

    /**
     * Claimant marks the decision as seen.
     */
    @PutMapping("/{claimId}/acknowledge")
    public ResponseEntity<Claim> acknowledgeClaimDecision(
            @PathVariable String claimId,
            Authentication authentication) {
        String claimantId = authentication.getName();
        return ResponseEntity.ok(claimService.acknowledgeDecision(claimId, claimantId));
    }

    // ---
    // Lookups
    // ---

    /**
     * All claims on one item.
     *
     * @param itemId the item
     * @return 200 with the claims
     */
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<Claim>> getClaimsByItem(@PathVariable String itemId) {
        List<Claim> claims = claimService.getClaimsByItem(itemId);
        return ResponseEntity.ok(claims);
    }

    /**
     * Claims made by the signed-in user.
     *
     * @param authentication the signed-in user
     * @return 200 with that user's claims
     */
    @GetMapping("/my-claims")
    public ResponseEntity<List<Claim>> getMyClaims(Authentication authentication) {
        String claimantId = authentication.getName();
        List<Claim> claims = claimService.getClaimsByClaimant(claimantId);
        return ResponseEntity.ok(claims);
    }

    /**
     * One claim by id.
     *
     * @param claimId the claim
     * @return 200 with the claim
     */
    @GetMapping("/{claimId}")
    public ResponseEntity<Claim> getClaimById(@PathVariable String claimId) {
        Claim claim = claimService.getClaimById(claimId);
        return ResponseEntity.ok(claim);
    }

    /**
     * Pending claims on items the signed-in user reported.
     */
    @GetMapping("/pending-on-my-items")
    public ResponseEntity<List<PendingClaimResponse>> getPendingClaimsOnMyItems(
            Authentication authentication) {
        String ownerId = authentication.getName();
        return ResponseEntity.ok(claimService.getPendingClaimsForOwner(ownerId));
    }

    /**
     * Contact for the other party on an approved claim.
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
