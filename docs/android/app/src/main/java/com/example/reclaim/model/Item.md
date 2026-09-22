# Item.java

## Overview
This file is a **model class**: a plain Java object that mirrors one kind of record the
backend stores — here, a single lost or found item. On the phone, one `Item` object
equals one item document on the server. See model and Gson in `docs/GLOSSARY.md`.
Unlike the request DTOs, `Item` is used in both directions: Gson fills it when parsing
item lists from the server, and the app also sends an `Item` as the body when creating
or updating a report.

## Concepts used here
- **Model.** A class that mirrors one stored record; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each Java field to the matching JSON key, both when
  reading server data and when sending; see `docs/GLOSSARY.md`.
- **Getters and setters.** Because Gson (and the screens) both read and write these
  fields, the class exposes plain getters and setters and a no-argument constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Every field is tagged so its
  JSON key is fixed regardless of how the Java field is named or minified.

## Fields
Each field is `@SerializedName`-tagged; the annotation's text is the JSON key.
- `id` — the item's unique id (server-assigned).
- `title` — short name of the item.
- `description` — longer text describing it.
- `category` — the item's category (e.g. electronics).
- `location` — human-readable place text.
- `status` — the item's state (for example open or closed).
- `closureReason` — why the item was closed, if it was.
- `type` — whether it is a "lost" or "found" report.
- `imageUrl` — URL of the item photo.
- `verificationQuestion` — the check question the owner sets for claimants.
- `latitude` / `longitude` — `Double` map coordinates (nullable, so they can be absent).
- `reportedAt` — when the report was made (a string timestamp).
- `ownerId` — id of the user who reported it (the server sets this).
- `ownerName` — display name of the owner.
- `ownerProfilePictureUrl` — the owner's profile photo URL.
- `targetLostReportId` — for a private "found" report aimed at a specific lost report,
  the id of that lost report.
- `hasUserAlreadyInteracted` — a `boolean` flag telling the UI whether the current
  viewer has already claimed or acted on this item.

## Constructors and accessors
- No-argument constructor `Item()` — used by Gson when parsing.
- Convenience constructor `Item(id, title, description, category, location, status,
  type, imageUrl, ownerId)` — sets the common fields when the app builds an item.
- Plain getters and setters for the fields (including `hasUserAlreadyInteracted()` as
  the boolean getter and `setHasUserAlreadyInteracted(...)`). These carry no logic, so
  they are not broken down one by one, per the DTO/model documentation style.

## Where it's used
Parsed from `getItems`, `getMyItems`, `getItemById`, and similar endpoints, and sent as
the body to `createItem`/`updateItem`. It is displayed by the home, search, my-items,
and details screens, and converted to/from `PendingReportEntity` for offline reports.
