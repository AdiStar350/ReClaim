# PasswordResetTokenRepository.java

## Overview
This file is a **Repository**: an interface that reads and writes one type of data in the database — here, `PasswordResetToken` records for the "forgot password" flow (see `docs/GLOSSARY.md`). Spring Data generates the queries from the method names, and inherited `MongoRepository` methods (`save`, `findById`, `delete`, ...) handle the rest.

## Concepts used here
- **Repository** — an interface Spring turns into database code for one model; see `docs/GLOSSARY.md`.
- **Spring Data derived query** — queries generated from method names.
- **`Optional`** — a container that holds a value or is empty, used instead of `null`.

## Libraries and imports
- **Spring Data MongoDB** (`MongoRepository`): extending `MongoRepository<PasswordResetToken, String>` provides `save`, `findById`, `delete`, etc. `AuthService` uses `save` and `delete` from here alongside the methods below. (Note: unlike the other repositories, this interface is not annotated `@Repository`; extending `MongoRepository` is enough for Spring to create it.)
- **Project class** (`com.example.reclaimbackend.model.PasswordResetToken`): the model stored.
- **Java standard** (`java.util.Optional`): return type for a lookup that may find nothing.

## Functions
> Declarations only; Spring Data generates each query from the method name.

### `Optional<PasswordResetToken> findByEmail(String email)`
```java
Optional<PasswordResetToken> findByEmail(String email);
```
- **What it does:** finds the reset record for an email, or empty if there isn't one.
- **Where it's called:** `AuthService.verifyPasswordResetOtp` and `AuthService.resetPassword`.
- **Why it's written this way:** `Optional` makes "no reset in progress" an explicit case the service must handle (it returns a `400`).
- **Step by step:** Spring matches on `email` and returns the record wrapped in `Optional`, or empty.

### `void deleteByEmail(String email)`
```java
void deleteByEmail(String email);
```
- **What it does:** deletes any reset record for an email.
- **Where it's called:** `AuthService.startPasswordReset`, to clear a leftover record before creating a fresh one.
- **Why it's written this way:** starting a new reset should wipe any stale one, so only one active record exists per email (also enforced by the unique index).
- **Step by step:** Spring builds a delete query on `email` and removes the matching document(s).
