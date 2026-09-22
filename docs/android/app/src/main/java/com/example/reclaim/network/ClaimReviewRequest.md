# ClaimReviewRequest.java

## Overview
This file is a **DTO (Data Transfer Object)** that carries an owner's decision on a
claim. A DTO holds one request body and fixes its JSON shape; see DTO and Gson in
`docs/GLOSSARY.md`. `ClaimReviewRequest` is the body for
`PUT api/claims/{claimId}/review`; it carries the new status the owner chose (for
example approved or rejected).

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps the `status` field to the JSON key `status`; see
  `docs/GLOSSARY.md`.
- **Immutable field (`final`).** The status is set once in the constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** `@SerializedName("status")`
  sets the JSON key.

## Fields
- `@SerializedName("status") private final String status` — the decision (a status
  string such as approved or rejected); sent as JSON key `status`.

Set by the constructor `ClaimReviewRequest(String status)`. There are no getters; the
object is only serialized and sent. The claim id itself is not in the body — it is put
in the URL path by the API method's `@Path` argument.

## Where it's used
Built in `PendingClaimsActivity` and `PendingClaimsSheet` when the owner approves or
rejects a claim, and passed to `ReClaimApiService.reviewClaim`.
