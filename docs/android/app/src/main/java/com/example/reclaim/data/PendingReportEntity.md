# PendingReportEntity.java

## Overview
This file is a **Room entity**: a plain class that maps to one row in a database table.
Each field becomes a column, and each object is one row; see Room in `docs/GLOSSARY.md`.
`PendingReportEntity` represents a report the user created while offline and that still
needs to be sent to the backend. It is stored in the `pending_reports` table until a
background worker uploads it.

## Concepts used here
- **Room entity.** A class mapped to a database table; see `docs/GLOSSARY.md`.
- **`@Entity(tableName = ...)`.** Marks the class as a table and names it.
- **`@PrimaryKey(autoGenerate = true)`.** Marks the id column and lets Room assign a new
  unique id on insert.
- **`@Nullable`.** Marks columns that may be empty in the database.
- **Public fields.** Room can read/write fields directly, so this entity uses public
  fields instead of getters/setters.

## Libraries and imports
- **AndroidX Room (`androidx.room.Entity`, `PrimaryKey`).** Define the table and its
  key.
- **AndroidX annotations (`androidx.annotation.Nullable`).** Marks nullable columns.
- **Project classes (`com.example.reclaim.model.Item`).** Used by `toItem()` to convert
  a stored row back into an `Item` for uploading.

## Fields
The class is annotated `@Entity(tableName = "pending_reports")`. Each public field is a
column:
- `@PrimaryKey(autoGenerate = true) public long id` — the row's unique id, assigned by
  Room.
- `title`, `description`, `category`, `location`, `type`, `verificationQuestion` — the
  report's text fields, mirroring the matching `Item` fields.
- `@Nullable targetLostReportId` — for a private found report, the lost report it
  targets.
- `@Nullable Double latitude`, `@Nullable Double longitude` — optional map coordinates.
- `@Nullable remoteImageUrl` — a photo URL if the image was already uploaded.
- `@Nullable localImagePath` — a path to a copy of the photo saved on the device, used
  when the image still needs uploading later.
- `@Nullable userId` — the owner of the queued report; `null` or empty means it was
  created before login and will be adopted on the next sign-in.
- `createdAtEpochMs` — when it was queued (used to send oldest first).

## Functions

### `public Item toItem()`
```java
public Item toItem() {
    Item item = new Item();
    item.setTitle(title);
    item.setDescription(description);
    item.setCategory(category);
    item.setLocation(location);
    item.setType(type);
    item.setVerificationQuestion(verificationQuestion);
    item.setLatitude(latitude);
    item.setLongitude(longitude);
    item.setImageUrl(remoteImageUrl);
    item.setTargetLostReportId(targetLostReportId);
    return item;
}
```
- **What it does:** builds an `Item` object from this stored row, copying the report
  fields across.
- **Where it's called:** `ReportSyncWorker.syncOne`, which turns the queued row into an
  `Item` and posts it to `createItem`.
- **Why it's written this way:** the network layer works with `Item`, not with the Room
  entity, so this converter bridges the two without leaking database types into the API
  call. It copies `remoteImageUrl` into the item's `imageUrl`; the worker fills that in
  first if only a local image path exists.
- **Step by step:**
  1. Create a new empty `Item`.
  2. Copy title, description, category, location, type, and the verification question.
  3. Copy latitude/longitude and the (remote) image URL.
  4. Copy the target lost report id.
  5. Return the filled `Item`.
