```java
@PostMapping
public ResponseEntity<Claim> submitClaim(
        @Valid @RequestBody ClaimRequest request,
        Authentication authentication) {

    String claimantId = authentication.getName();
    Claim claim = claimService.submitClaim(request, claimantId);

    return ResponseEntity.status(HttpStatus.CREATED).body(claim);
}
```

What that function does: I save a new claim for the signed-in user.
What the function is responsible for: I am POST /api/claims.
Where it works: I am an HTTP endpoint. I call ClaimService.submitClaim.
Why I chose to write the code of the function the way its written: I take claimantId from the token, not the body, so a caller cannot claim as someone else. 201 marks a new claim.
More: I use @Valid ClaimRequest, Authentication.getName, and HttpStatus.CREATED.

```java
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
```

What that function does: I approve or decline a claim as the item owner.
What the function is responsible for: I am PUT /api/claims/{claimId}/review.
Where it works: I am an HTTP endpoint. I call ClaimService.reviewClaim. ClaimServiceTest checks that service method.
Why I chose to write the code of the function the way its written: I pass reviewerId from the token so only the owner can review. Status comes from the validated body.
More: I use ClaimReviewRequest.getStatus and @PathVariable claimId.

```java
@PutMapping("/{claimId}/acknowledge")
public ResponseEntity<Claim> acknowledgeClaimDecision(
        @PathVariable String claimId,
        Authentication authentication) {
    String claimantId = authentication.getName();
    return ResponseEntity.ok(claimService.acknowledgeDecision(claimId, claimantId));
}
```

What that function does: I mark the claim decision as seen by the claimant.
What the function is responsible for: I am PUT /api/claims/{claimId}/acknowledge.
Where it works: I am an HTTP endpoint. I call ClaimService.acknowledgeDecision.
Why I chose to write the code of the function the way its written: I pass the token user id so only the claimant can dismiss their own card.
More: I use Authentication.getName and ResponseEntity.ok.

```java
@GetMapping("/item/{itemId}")
public ResponseEntity<List<Claim>> getClaimsByItem(@PathVariable String itemId) {
    List<Claim> claims = claimService.getClaimsByItem(itemId);
    return ResponseEntity.ok(claims);
}
```

What that function does: I return all claims on one item.
What the function is responsible for: I am GET /api/claims/item/{itemId}.
Where it works: I am an HTTP endpoint. I call ClaimService.getClaimsByItem.
Why I chose to write the code of the function the way its written: I keep this lookup thin. The service does the repository call.
More: I use @PathVariable itemId. This route still needs a signed-in user.

```java
@GetMapping("/my-claims")
public ResponseEntity<List<Claim>> getMyClaims(Authentication authentication) {
    String claimantId = authentication.getName();
    List<Claim> claims = claimService.getClaimsByClaimant(claimantId);
    return ResponseEntity.ok(claims);
}
```

What that function does: I return claims made by the signed-in user.
What the function is responsible for: I am GET /api/claims/my-claims.
Where it works: I am an HTTP endpoint. I call ClaimService.getClaimsByClaimant.
Why I chose to write the code of the function the way its written: I take the id from the token so the list is always "my" claims.
More: I use Authentication.getName.

```java
@GetMapping("/{claimId}")
public ResponseEntity<Claim> getClaimById(@PathVariable String claimId) {
    Claim claim = claimService.getClaimById(claimId);
    return ResponseEntity.ok(claim);
}
```

What that function does: I return one claim by id.
What the function is responsible for: I am GET /api/claims/{claimId}.
Where it works: I am an HTTP endpoint. I call ClaimService.getClaimById.
Why I chose to write the code of the function the way its written: I let the service throw 404 if the claim is missing.
More: I use @PathVariable claimId. This route still needs a signed-in user.

```java
@GetMapping("/pending-on-my-items")
public ResponseEntity<List<PendingClaimResponse>> getPendingClaimsOnMyItems(
        Authentication authentication) {
    String ownerId = authentication.getName();
    return ResponseEntity.ok(claimService.getPendingClaimsForOwner(ownerId));
}
```

What that function does: I return pending claims on items this user reported.
What the function is responsible for: I am GET /api/claims/pending-on-my-items.
Where it works: I am an HTTP endpoint. I call ClaimService.getPendingClaimsForOwner.
Why I chose to write the code of the function the way its written: I pass the owner id from the token so the review list is only for this user's items.
More: I return List of PendingClaimResponse.

```java
@GetMapping("/{claimId}/contact")
public ResponseEntity<ContactResponse> getClaimContact(
        @PathVariable String claimId,
        Authentication authentication) {
    String requesterId = authentication.getName();
    ContactResponse contact = claimService.getContactForApprovedClaim(
            claimId, requesterId);
    return ResponseEntity.ok(contact);
}
```

What that function does: I return contact for the other party on an approved claim.
What the function is responsible for: I am GET /api/claims/{claimId}/contact.
Where it works: I am an HTTP endpoint. I call ClaimService.getContactForApprovedClaim. ClaimServiceTest checks that service method.
Why I chose to write the code of the function the way its written: I pass requesterId so the service can allow only owner or claimant.
More: I use ContactResponse and Authentication.getName.
