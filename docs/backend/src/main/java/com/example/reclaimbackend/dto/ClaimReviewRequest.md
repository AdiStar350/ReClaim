# ClaimReviewRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class that only carries data in or out of the API, kept separate from the database model (see `docs/GLOSSARY.md`). `ClaimReviewRequest` is the JSON body the item's owner sends when approving or declining a claim. It carries a single value, `status`, which must be exactly `"APPROVED"` or `"REJECTED"`.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Validation** — annotations reject bad input before our code runs; see `docs/GLOSSARY.md`.
- **Lombok** — generates boilerplate (getters, setters, constructors, builder); see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`jakarta.validation.constraints.NotBlank`, `jakarta.validation.constraints.Pattern`): `@NotBlank` requires a value; `@Pattern` requires the value to match a regular expression (here, one of two exact words). Spring runs these checks because the controller marks the body `@Valid`.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors, an empty constructor, an all-fields constructor, and a `builder()` factory. The empty constructor + setters let the JSON library build the object from the request.

## Fields
```java
@NotBlank(message = "Status is required")
@Pattern(regexp = "APPROVED|REJECTED",
        message = "Status must be either APPROVED or REJECTED")
private String status;
```
- `status` — the owner's decision. Only `"APPROVED"` or `"REJECTED"` are accepted.

**Annotations used and why:**
- `@NotBlank` — the field must be present and non-empty.
- `@Pattern(regexp = "APPROVED|REJECTED")` — limits the value to those two words so a typo or unexpected status can't reach the service; a bad value returns `400 Bad Request`.
- `@Getter`/`@Setter` — create `getStatus()`/`setStatus(...)`.
- `@NoArgsConstructor` — empty constructor the JSON library needs to build the object.
- `@AllArgsConstructor` / `@Builder` — convenient ways to create the object in code and tests.

Spring builds this object from the JSON body of `PUT /api/claims/{claimId}/review` in `ClaimController.reviewClaim`, which then reads `getStatus()` and passes it to `ClaimService.reviewClaim`.
