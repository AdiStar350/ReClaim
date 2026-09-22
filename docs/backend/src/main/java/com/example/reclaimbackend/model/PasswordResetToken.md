# PasswordResetToken.java

## Overview
This file is a **Model** (entity): a plain class that mirrors one stored record. One `PasswordResetToken` object equals one document in the MongoDB `password_reset_otps` collection (see `docs/GLOSSARY.md`). It is a short-lived record that tracks a "forgot password" attempt: the hashed email code, how many wrong tries have happened, and — after the code is accepted — the random reset token used to actually set the new password.

## Concepts used here
- **Model / entity** — a class that represents one stored record; see `docs/GLOSSARY.md`.
- **MongoDB document & collection** — a JSON-like record in a named group; see `docs/GLOSSARY.md`.
- **OTP** — the emailed 6-digit code; stored here only as a hash; see `docs/GLOSSARY.md`.
- **Hashing** — `otpHash` stores a scrambled version of the code, so the raw code isn't kept; see `docs/GLOSSARY.md`.
- **Expiry & attempt limit** — the record has an expiry time and an attempt counter to resist guessing.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Data MongoDB / Commons** (`Document`, `Id`, `Indexed`): `@Document(collection = "password_reset_otps")` picks the collection; `@Id` marks the primary key; `@Indexed(unique = true)` on `email` keeps at most one active reset record per email.
- **Java standard** (`java.time.Instant`): precise timestamps for `createdAt` and `expiresAt`.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): accessors, constructors, and a builder used in `AuthService`.

## Fields
```java
@Id
private String id;

@Indexed(unique = true)
private String email;

/** Hashed 6-digit code. */
private String otpHash;

/** How many wrong codes have been tried. */
private int attempts;

/** True after the code was accepted. */
private boolean verified;

/** Random token given after the code is accepted. May be empty. */
private String resetToken;

private Instant createdAt;

private Instant expiresAt;
```
- `id` — primary key.
- `email` — the account resetting its password; unique so one email has one live reset record.
- `otpHash` — the hashed emailed code; the typed code is hashed and compared, so the plain code is never stored.
- `attempts` — count of wrong codes; once it hits the max, the record is thrown out.
- `verified` — becomes `true` after the code is accepted.
- `resetToken` — a random token issued after verification; the final reset step must present it. Empty until then.
- `createdAt` — when the request started.
- `expiresAt` — when the code/token stops working; the service checks this before trusting it.

**Annotations used and why:**
- `@Document(collection = "password_reset_otps")` — maps to that collection.
- `@Id` — marks `id` as primary key.
- `@Indexed(unique = true)` on `email` — enforces one active record per email.
- `@Getter`/`@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` — Lombok boilerplate used by `AuthService` to create and update the record.
