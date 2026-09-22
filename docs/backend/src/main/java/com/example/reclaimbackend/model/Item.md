# Item.java

## Overview
This file is a **Model** (entity): a plain class that mirrors one stored record. One `Item` object equals one document in the MongoDB `items` collection (see `docs/GLOSSARY.md`). An item is one lost or found report. Its `ownerId` is the person who reported it. Several fields are `@Indexed` so search and filters stay fast, and a few are `@Transient` (filled on read, never saved).

## Concepts used here
- **Model / entity** — a class that represents one stored record; see `docs/GLOSSARY.md`.
- **MongoDB document & collection** — a JSON-like record in a named group; see `docs/GLOSSARY.md`.
- **Index** — a database structure that makes lookups/filters/sorts faster; see `docs/GLOSSARY.md`.
- **Transient field** — a field kept only in memory for the response, not written to the database.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Data MongoDB / Commons** (`Document`, `Id`, `Indexed`, `Transient`): `@Document(collection = "items")` stores this class in the `items` collection; `@Id` marks the primary key; `@Indexed` adds a single-field index for faster queries; `@Transient` marks fields not to save.
- **Java standard** (`java.time.Instant`): `Instant` is a precise point in time (UTC) used for `reportedAt`.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors, constructors, and a builder.

## Fields
```java
@Id
private String id;

private String title;

private String description;

@Indexed
private String category;

private String location;

/** "OPEN" or "CLOSED". */
@Indexed
private String status;

/** Why it was closed. See {@link ClosureReasons}. Empty while still open. */
private String closureReason;

/** "Lost" or "Found". */
@Indexed
private String type;

/** Photo URL. */
private String imageUrl;

/** Hidden question set by the reporter. */
private String verificationQuestion;

private Double latitude;

private Double longitude;

@Indexed
private Instant reportedAt;

/** Who reported this item. */
private String ownerId;

/**
 * If set, this found report is only for the owner of that lost report.
 * It stays off public search and map lists.
 */
@Indexed
private String targetLostReportId;

/** Reporter's name. Filled on read, not saved. */
@Transient
private String ownerName;

/** Reporter's picture URL. Filled on read, not saved. */
@Transient
private String ownerProfilePictureUrl;

/**
 * True if this viewer already claimed a found report
 * or filed a found report for this lost item.
 * Filled on read, not saved.
 */
@Transient
private Boolean hasUserAlreadyInteracted;
```
- `id` — primary key.
- `title` / `description` — what was lost/found and its details.
- `category` — grouping (e.g. "Wallets"); indexed because search filters by it.
- `location` — free-text place description.
- `status` — `"OPEN"` or `"CLOSED"`; indexed because most queries hide closed items.
- `closureReason` — why it closed (from `ClosureReasons`); empty while open.
- `type` — `"Lost"` or `"Found"`; indexed because queries filter by it.
- `imageUrl` — photo link.
- `verificationQuestion` — the reporter's hidden "prove it's yours" question.
- `latitude` / `longitude` — map coordinates (`Double`, so they may be null if not set).
- `reportedAt` — when it was reported; indexed and used to sort newest-first.
- `ownerId` — the reporter's user id.
- `targetLostReportId` — if set, this found report is *private* to one specific lost report's owner; such items are hidden from public search and map.
- `ownerName`, `ownerProfilePictureUrl` — **transient** reporter details filled in when returning items, so the app can show who reported it.
- `hasUserAlreadyInteracted` — **transient** flag telling the current viewer whether they already claimed/filed against this item, so the app can adjust its buttons.

**Annotations used and why:**
- `@Document(collection = "items")` — maps this class to the `items` collection.
- `@Id` — marks `id` as the primary key.
- `@Indexed` on `category`, `status`, `type`, `reportedAt`, `targetLostReportId` — speeds up the filters and sorts these fields are used in. (See also `MongoIndexConfig`, which builds compound indexes covering several of these together.)
- `@Transient` — keeps `ownerName`, `ownerProfilePictureUrl`, and `hasUserAlreadyInteracted` out of the stored document.
- `@Getter`/`@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` — Lombok boilerplate used across `ItemService` and tests.
