# PendingClaimResponse.java

## Overview
This file is a **response DTO**: a small class that carries data back from an API call.
See DTO and Gson in `docs/GLOSSARY.md`. `PendingClaimResponse` is a richer view of a
claim, built for the item owner who is reviewing claims. Instead of just claim ids, it
also carries the item's title/image, the claimant's name, and the verification
question — everything the owner needs to decide, in one object, without extra fetches.

## Concepts used here
- **DTO (response side).** A carrier object for one response body; see
  `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each JSON key to its Java field when parsing; see
  `docs/GLOSSARY.md`.
- **"Joined" view.** This DTO combines fields from the claim, the item, and the
  claimant so the owner's review screen can render everything at once.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Tags each field with its
  JSON key.

## Fields
Each field is `@SerializedName`-tagged with the JSON key shown by its name.
- `claimId` — the claim's id (used when approving/rejecting).
- `itemId` — the claimed item's id.
- `itemTitle` — the item's title, for display.
- `itemImageUrl` — the item's photo URL, for display.
- `claimantId` — the claiming user's id.
- `claimantName` — the claiming user's name, for display.
- `validationAnswer` — the claimant's answer to the verification question.
- `status` — the claim's current state.
- `verificationQuestion` — the check question the owner set, shown next to the answer.

Fields are not `final` because Gson fills them. The only methods are plain getters with
no logic, so they are not documented one by one, per the DTO style.

## Where it's used
Returned by `getPendingClaimsOnMyItems` and shown in `PendingClaimsActivity`,
`PendingClaimsSheet`, and the claim card in `HomeFragment`.
