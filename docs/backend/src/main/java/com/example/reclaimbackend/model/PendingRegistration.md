# PendingRegistration.java

## Overview
This file is a **Model** (entity): a plain class that mirrors one stored record. One `PendingRegistration` object equals one document in the MongoDB `pending_registrations` collection (see `docs/GLOSSARY.md`). It holds a signup's details **before** the email code is confirmed. Once the code is verified, the backend creates a real `User` from it and deletes this record. Keeping unverified signups here means the `users` collection only ever contains confirmed accounts.

## Concepts used here
- **Model / entity** — a class that represents one stored record; see `docs/GLOSSARY.md`.
- **MongoDB document & collection** — a JSON-like record in a named group; see `docs/GLOSSARY.md`.
- **OTP** — the emailed 6-digit code; stored here only as a hash; see `docs/GLOSSARY.md`.
- **Hashing** — both the password and the code are stored hashed, never in plain text; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Data MongoDB / Commons** (`Document`, `Id`, `Indexed`): `@Document(collection = "pending_registrations")` picks the collection; `@Id` marks the primary key; `@Indexed(unique = true)` on `email` keeps one pending signup per email.
- **Java standard** (`java.time.Instant`): timestamps for `createdAt` and `expiresAt`.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): accessors, constructors, and a builder used in `AuthService`.

## Fields
```java
@Id
private String id;

@Indexed(unique = true)
private String email;

/** Hashed password. Copied onto the user later. */
private String passwordHash;

private String name;

private String phoneNumber;

/** Profile picture URL. May be empty. */
private String profilePictureUrl;

/** Hashed 6-digit code. */
private String otpHash;

/** How many wrong codes have been tried. */
private int attempts;

private Instant createdAt;

private Instant expiresAt;
```
- `id` — primary key.
- `email` — the signup email; unique so there's one pending record per email.
- `passwordHash` — the already-hashed password, copied straight onto the new `User` when the code is verified.
- `name` / `phoneNumber` — the user's details, saved for when the account is created.
- `profilePictureUrl` — optional profile photo link.
- `otpHash` — the hashed emailed code; the typed code is hashed and compared.
- `attempts` — count of wrong codes; once it hits the max, the record is deleted.
- `createdAt` — when the signup started.
- `expiresAt` — when the code stops working; verification checks this first.

**Annotations used and why:**
- `@Document(collection = "pending_registrations")` — maps to that collection.
- `@Id` — marks `id` as primary key.
- `@Indexed(unique = true)` on `email` — one pending signup per email.
- `@Getter`/`@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` — Lombok boilerplate used by `AuthService` to build and update the pending record.
