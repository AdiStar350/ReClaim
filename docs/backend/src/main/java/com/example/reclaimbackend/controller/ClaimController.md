# ClaimController.java

## Overview
This is a **controller** for ownership **claims** on lost/found items (see `docs/GLOSSARY.md`). Routes under `/api/claims` require a signed-in user (JWT). The controller reads the user id from Spring Security's `Authentication`, never from the request body, then delegates create/review/list/contact work to `ClaimService`.

## Concepts used here
- **Controller / endpoint / REST** — HTTP doors for claim actions; see `docs/GLOSSARY.md`.
- **Authentication vs authorization** — JWT proves who is calling; the service checks ownership for reviews; see `docs/GLOSSARY.md`.
- **DTO** — `ClaimRequest`, `ClaimReviewRequest`, `PendingClaimResponse`, `ContactResponse`; see `docs/GLOSSARY.md`.
- **Validation** — `@Valid` on request bodies; see `docs/GLOSSARY.md`.
- **Dependency injection** — `ClaimService` is injected; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Web** (`@RestController`, mappings, `ResponseEntity`, `@PathVariable`, `@RequestBody`): HTTP routing and responses.
- **Spring Security** (`Authentication`): provides the signed-in user id via `getName()`.
- **Jakarta Validation** (`@Valid`): validates DTOs.
- **Lombok** (`@RequiredArgsConstructor`): injects `claimService`.
- **Project types**: claim DTOs/model and `ClaimService`.

## Fields
- `claimService` (`ClaimService`) — business rules for submitting, reviewing, and listing claims.

## Functions

### `public ResponseEntity<Claim> submitClaim(@Valid @RequestBody ClaimRequest request, Authentication authentication)`
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
- **What it does:** creates a new claim for the signed-in user on the item named in the body.
- **Where it's called:** by Spring MVC on `POST /api/claims`.
- **Why it's written this way:** taking `claimantId` from the token stops clients from claiming as someone else.
- **Step by step:**
  1. Read claimant id from `authentication.getName()`.
  2. Call `claimService.submitClaim`.
  3. Return `201` with the saved `Claim`.

### `public ResponseEntity<Claim> reviewClaim(@PathVariable String claimId, @Valid @RequestBody ClaimReviewRequest request, Authentication authentication)`
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
- **What it does:** approves or rejects a claim; the service enforces that only the item owner may review.
- **Where it's called:** by Spring MVC on `PUT /api/claims/{claimId}/review`.
- **Why it's written this way:** status comes from the DTO; identity comes from the token so review rights cannot be spoofed in the body.
- **Step by step:**
  1. Read reviewer id from authentication.
  2. Call `claimService.reviewClaim` with claim id, status, and reviewer id.
  3. Return `200` with the updated claim.

### `public ResponseEntity<Claim> acknowledgeClaimDecision(@PathVariable String claimId, Authentication authentication)`
```java
@PutMapping("/{claimId}/acknowledge")
public ResponseEntity<Claim> acknowledgeClaimDecision(
        @PathVariable String claimId,
        Authentication authentication) {
    String claimantId = authentication.getName();
    return ResponseEntity.ok(claimService.acknowledgeDecision(claimId, claimantId));
}
```
- **What it does:** lets the claimant mark an approve/reject decision as seen.
- **Where it's called:** by Spring MVC on `PUT /api/claims/{claimId}/acknowledge`.
- **Why it's written this way:** acknowledgment is a separate step so the app can clear UI prompts after the user has read the result.
- **Step by step:**
  1. Read claimant id from authentication.
  2. Call `claimService.acknowledgeDecision`.
  3. Return the updated claim.

### `public ResponseEntity<List<Claim>> getClaimsByItem(@PathVariable String itemId)`
```java
@GetMapping("/item/{itemId}")
public ResponseEntity<List<Claim>> getClaimsByItem(@PathVariable String itemId) {
    List<Claim> claims = claimService.getClaimsByItem(itemId);
    return ResponseEntity.ok(claims);
}
```
- **What it does:** lists all claims for one item.
- **Where it's called:** by Spring MVC on `GET /api/claims/item/{itemId}`.
- **Why it's written this way:** a simple pass-through keeps listing logic in the service/repository.
- **Step by step:**
  1. Call `claimService.getClaimsByItem`.
  2. Return `200` with the list.

### `public ResponseEntity<List<Claim>> getMyClaims(Authentication authentication)`
```java
@GetMapping("/my-claims")
public ResponseEntity<List<Claim>> getMyClaims(Authentication authentication) {
    String claimantId = authentication.getName();
    List<Claim> claims = claimService.getClaimsByClaimant(claimantId);
    return ResponseEntity.ok(claims);
}
```
- **What it does:** lists claims filed by the signed-in user.
- **Where it's called:** by Spring MVC on `GET /api/claims/my-claims`.
- **Why it's written this way:** the user id comes from the token so a client cannot ask for another user's claim history by id.
- **Step by step:**
  1. Read claimant id from authentication.
  2. Call `claimService.getClaimsByClaimant`.
  3. Return the list.

### `public ResponseEntity<Claim> getClaimById(@PathVariable String claimId)`
```java
@GetMapping("/{claimId}")
public ResponseEntity<Claim> getClaimById(@PathVariable String claimId) {
    Claim claim = claimService.getClaimById(claimId);
    return ResponseEntity.ok(claim);
}
```
- **What it does:** returns one claim by id (service throws if missing).
- **Where it's called:** by Spring MVC on `GET /api/claims/{claimId}`.
- **Why it's written this way:** thin controller; not-found handling lives in the service.
- **Step by step:**
  1. Call `claimService.getClaimById`.
  2. Return `200` with the claim.

### `public ResponseEntity<List<PendingClaimResponse>> getPendingClaimsOnMyItems(Authentication authentication)`
```java
@GetMapping("/pending-on-my-items")
public ResponseEntity<List<PendingClaimResponse>> getPendingClaimsOnMyItems(
        Authentication authentication) {
    String ownerId = authentication.getName();
    return ResponseEntity.ok(claimService.getPendingClaimsForOwner(ownerId));
}
```
- **What it does:** returns pending claims on items the signed-in user reported, as richer DTOs for the UI.
- **Where it's called:** by Spring MVC on `GET /api/claims/pending-on-my-items`.
- **Why it's written this way:** the owner id comes from the token; the service joins claim + item details into `PendingClaimResponse`.
- **Step by step:**
  1. Read owner id from authentication.
  2. Call `claimService.getPendingClaimsForOwner`.
  3. Return the list.

### `public ResponseEntity<ContactResponse> getClaimContact(@PathVariable String claimId, Authentication authentication)`
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
- **What it does:** returns the other party's contact info only when the claim is approved and the caller is involved.
- **Where it's called:** by Spring MVC on `GET /api/claims/{claimId}/contact`.
- **Why it's written this way:** contact details are gated in the service so random users cannot scrape phone/email from claim ids.
- **Step by step:**
  1. Read requester id from authentication.
  2. Call `claimService.getContactForApprovedClaim`.
  3. Return `ContactResponse`.
