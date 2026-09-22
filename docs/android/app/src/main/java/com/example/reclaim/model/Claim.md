# Claim.java

## Overview
This file is a **model class**: a plain Java object that mirrors one claim record from
the backend. A claim is one person's request to receive a lost/found item. One `Claim`
object equals one claim document on the server. See model and Gson in
`docs/GLOSSARY.md`. Gson fills a `Claim` when parsing claim lists, and the app reads its
fields to show claim cards and results.

## Concepts used here
- **Model.** A class that mirrors one stored record; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each Java field to the matching JSON key when
  parsing; see `docs/GLOSSARY.md`.
- **Getters and setters.** Gson writes the fields and the UI reads them, so the class
  exposes plain accessors and a no-argument constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Tags each field with its
  JSON key.

## Fields
Each field is `@SerializedName`-tagged with the JSON key shown by its name.
- `id` — the claim's unique id.
- `itemId` — id of the item being claimed.
- `claimantId` — id of the user making the claim.
- `validationAnswer` — the claimant's answer to the item's verification question.
- `status` — the claim's state (for example `Pending`, `Approved`, `Rejected`).
- `closureReason` — why the claim was closed, if applicable.
- `itemTitle` — the item's title, copied in so a claim can be shown without a second
  fetch.
- `itemImageUrl` — the item's photo URL, copied in for the same reason.
- `itemType` — whether the item is lost or found.
- `decisionAcknowledged` — a `boolean` telling whether the claimant has already seen
  the approve/reject decision.

The last few item-related fields are stored on the claim so a claim card can render on
its own without loading the whole item.

## Constructors and accessors
- No-argument constructor `Claim()` — used by Gson.
- Convenience constructor `Claim(id, itemId, claimantId, validationAnswer, status)` —
  sets the main fields.
- Plain getters and setters (with `isDecisionAcknowledged()` as the boolean getter).
  These carry no logic and so are not documented one by one, per the DTO/model style.

## Where it's used
Parsed from `getMyClaims`, `submitClaim`, `reviewClaim`, and
`acknowledgeClaimDecision`. Shown in `HomeFragment`, `ReportHistoryActivity`, and the
claim dialogs.
