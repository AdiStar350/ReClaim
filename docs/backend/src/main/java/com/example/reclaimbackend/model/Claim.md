# Claim.java

## Overview
This file is a **Model** (entity): a plain class that mirrors one stored record. One `Claim` object equals one document in the MongoDB `claims` collection (see `docs/GLOSSARY.md`). A claim is one person saying "this reported item is mine." The item's reporter later approves or declines it. Some fields are stored in the database; a few are marked `@Transient`, meaning they are filled in only when reading (to help the app) and never saved.

## Concepts used here
- **Model / entity** — a class that represents one stored record; see `docs/GLOSSARY.md`.
- **MongoDB document & collection** — a JSON-like record in a named group; see `docs/GLOSSARY.md`.
- **Transient field** — a field kept only in memory for the response, not written to the database.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Data MongoDB / Commons** (`Document`, `Id`, `Transient`): `@Document(collection = "claims")` stores this class in the `claims` collection; `@Id` marks the primary key; `@Transient` marks fields that should **not** be saved (they are populated on read).
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors, constructors, and the `builder()` used in `ClaimService` and tests.

## Fields
```java
@Id
private String id;

/** The item being claimed. */
private String itemId;

/** Who made the claim. */
private String claimantId;

/** Answer to the reporter's hidden question. */
private String validationAnswer;

/** "Pending", "Approved", or "Rejected". */
private String status;

/** Why it ended. See {@link ClosureReasons}. Empty while still pending. */
private String closureReason;

/** Whether the claimant has dismissed the decision card. */
private boolean decisionAcknowledged;

/** Item title. Filled on read, not saved. */
@Transient
private String itemTitle;

/** Item photo URL. Filled on read, not saved. */
@Transient
private String itemImageUrl;

/** "Lost" or "Found". Filled on read, not saved. */
@Transient
private String itemType;
```
- `id` — the claim's primary key.
- `itemId` — the item this claim is about.
- `claimantId` — the user who filed the claim (taken from their login token, not user input).
- `validationAnswer` — the claimant's answer to the reporter's hidden "prove it" question.
- `status` — the claim state: pending, approved, or rejected (stored uppercase in practice, e.g. `"PENDING"`).
- `closureReason` — why it ended (from `ClosureReasons`, e.g. claim approved/declined); empty while pending.
- `decisionAcknowledged` — `true` once the claimant has seen the decision, so the home-screen card can disappear.
- `itemTitle`, `itemImageUrl`, `itemType` — **transient** copies of item details, filled in only when the service returns claims to the app, so the app can show the item without extra requests. They are never written to the `claims` collection.

**Annotations used and why:**
- `@Document(collection = "claims")` — maps this class to the `claims` collection.
- `@Id` — marks `id` as the primary key.
- `@Transient` — keeps `itemTitle`/`itemImageUrl`/`itemType` out of the saved document; they exist only for the response.
- `@Getter`/`@Setter` — accessors used across the claim flow.
- `@Builder` — builds claims in `ClaimService.submitClaim` and tests.
- `@NoArgsConstructor` / `@AllArgsConstructor` — empty and all-fields constructors (Mongo uses the no-arg one when loading).
