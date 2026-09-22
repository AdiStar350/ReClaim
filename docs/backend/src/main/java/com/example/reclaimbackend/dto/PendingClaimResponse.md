# PendingClaimResponse.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `PendingClaimResponse` is an **output** DTO built for the owner's "claims waiting for my review" screen. It joins together a bit of the `Claim` and a bit of the `Item` into one flat object, so the app doesn't have to make several calls to show the review list.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **"View model" / joined response** — one DTO that combines fields from more than one database record for a single screen.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors, constructors, and the `builder()` the service uses to fill each field. No validation, because this DTO is only sent out.

## Fields
```java
private String claimId;
private String itemId;
private String itemTitle;
private String itemImageUrl;
private String claimantId;
private String claimantName;
private String validationAnswer;
private String status;

/** The owner's hidden question for this item. */
private String verificationQuestion;
```
- `claimId` — id of the claim being reviewed.
- `itemId` — id of the claimed item.
- `itemTitle` — the item's title, so the owner recognizes it.
- `itemImageUrl` — the item's photo URL for the list.
- `claimantId` — id of the person who made the claim.
- `claimantName` — the claimant's display name (looked up from the user record).
- `validationAnswer` — the claimant's answer to the hidden question.
- `verificationQuestion` — the owner's own hidden "prove it" question, shown next to the answer so the owner can judge it.
- `status` — the claim's status (here always the pending state, since this list is pending claims).

**Annotations used and why:**
- `@Getter`/`@Setter` — create getters (read by the JSON library) and setters.
- `@Builder` — lets the service build each response with `PendingClaimResponse.builder()...build()`.
- `@NoArgsConstructor` / `@AllArgsConstructor` — empty and all-fields constructors.

This object is built by `ClaimService.getPendingClaimsForOwner` and returned (as a list) from `GET /api/claims/pending-on-my-items` in `ClaimController.getPendingClaimsOnMyItems`.
