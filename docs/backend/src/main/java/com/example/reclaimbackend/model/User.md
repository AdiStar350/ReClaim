# User.java

## Overview
This file is a **Model** (also called an entity): a plain class that mirrors one kind of stored record. One `User` object equals one document in the MongoDB `users` collection (see `docs/GLOSSARY.md` for Model and MongoDB). It holds everything the backend keeps about a person, including the hashed password. Because it contains the password hash, the backend converts a `User` to a `UserResponse` DTO before sending it to the app.

## Concepts used here
- **Model / entity** — a class that represents one stored record; see `docs/GLOSSARY.md`.
- **MongoDB document & collection** — a JSON-like record stored in a named group; see `docs/GLOSSARY.md`.
- **Index (unique)** — a database rule that speeds up lookups and, here, forbids two users with the same email; see `docs/GLOSSARY.md`.
- **Password hashing** — `password` stores a scrambled one-way hash, never the real password; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Data MongoDB** (`org.springframework.data.mongodb.core.mapping.Document`, `org.springframework.data.mongodb.core.index.Indexed`) and **Spring Data Commons** (`org.springframework.data.annotation.Id`): `@Document(collection = "users")` says objects of this class are stored in the `users` collection; `@Id` marks the field Mongo uses as the primary key `_id`; `@Indexed(unique = true)` builds a unique index so duplicate emails are rejected by the database.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors, constructors, and the `builder()` used across the services (for example `User.builder().email(...).build()`).

## Fields
```java
@Id
private String id;

@Indexed(unique = true)
private String email;

private String password;

private String name;

private String phoneNumber;

/** Device token for push notifications. May be empty. */
private String fcmToken;

/** Profile picture URL. May be empty. */
private String profilePictureUrl;
```
- `id` — the document's primary key (Mongo's `_id`).
- `email` — the login email; the unique index guarantees no two users share one.
- `password` — the **hashed** password (via BCrypt), never the plain text.
- `name` — display name.
- `phoneNumber` — phone number (shown to the other party after an approved claim).
- `fcmToken` — the phone's push-notification address; may be empty until the app registers it.
- `profilePictureUrl` — link to the profile photo; may be empty.

**Annotations used and why:**
- `@Document(collection = "users")` — maps this class to the `users` collection.
- `@Id` — marks `id` as the primary key.
- `@Indexed(unique = true)` on `email` — enforces one account per email at the database level, on top of the app's own checks.
- `@Getter`/`@Setter` — accessors used everywhere the code reads or changes a user.
- `@Builder` — used by `AuthService` and tests to build users field by field.
- `@NoArgsConstructor` / `@AllArgsConstructor` — empty and all-fields constructors (Mongo uses the no-arg one when loading).
