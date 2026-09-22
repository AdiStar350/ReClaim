# ClaimRequest.java

## Overview
This file is a **DTO (Data Transfer Object)** that carries a claim on an item. A DTO
holds one request body and fixes its JSON shape; see DTO and Gson in
`docs/GLOSSARY.md`. `ClaimRequest` is the body for `POST api/claims`: which item is
being claimed and the answer to the owner's verification question (the check that helps
prove the claimant really owns the item).

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each field to its JSON key; see `docs/GLOSSARY.md`.
- **Verification answer.** The claimant's reply to the item's check question, used by
  the owner to judge the claim.
- **Immutable fields (`final`).** Both values are set once in the constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Sets the JSON keys `itemId`
  and `validationAnswer`.

## Fields
- `@SerializedName("itemId") private final String itemId` — the id of the item being
  claimed.
- `@SerializedName("validationAnswer") private final String validationAnswer` — the
  claimant's answer to the item's verification question.

Set by the constructor `ClaimRequest(String itemId, String validationAnswer)`. Plain
getters (`getItemId`, `getValidationAnswer`) return each field with no logic.

## Where it's used
Built in `ItemDetailsActivity` when the user submits a claim, and passed to
`ReClaimApiService.submitClaim`.
