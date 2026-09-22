# PendingRegistrationRepository.java

## Overview
This file is a **Repository**: an interface that reads and writes one type of data in the database — here, `PendingRegistration` records (signups waiting for their email code) (see `docs/GLOSSARY.md`). Spring Data generates the queries from the method names, and inherited `MongoRepository` methods (`save`, `delete`, ...) handle the rest.

## Concepts used here
- **Repository** — an interface Spring turns into database code for one model; see `docs/GLOSSARY.md`.
- **Spring Data derived query** — queries generated from method names.
- **`Optional`** — a container that holds a value or is empty, used instead of `null`.

## Libraries and imports
- **Spring Data MongoDB** (`MongoRepository`): extending `MongoRepository<PendingRegistration, String>` provides `save`, `delete`, `findById`, etc. `AuthService` uses `save` and `delete` from here alongside the methods below. (Like `PasswordResetTokenRepository`, it isn't annotated `@Repository`; extending `MongoRepository` is enough.)
- **Project class** (`com.example.reclaimbackend.model.PendingRegistration`): the model stored.
- **Java standard** (`java.util.Optional`): return type for a lookup that may find nothing.

## Functions
> Declarations only; Spring Data generates each query from the method name.

### `Optional<PendingRegistration> findByEmail(String email)`
```java
Optional<PendingRegistration> findByEmail(String email);
```
- **What it does:** finds the pending signup for an email, or empty if there isn't one.
- **Where it's called:** `AuthService.resendOtp` and `AuthService.verifyRegistration`.
- **Why it's written this way:** `Optional` makes "no pending signup" an explicit case (the service returns a `400`).
- **Step by step:** Spring matches on `email` and returns the record wrapped in `Optional`, or empty.

### `void deleteByEmail(String email)`
```java
void deleteByEmail(String email);
```
- **What it does:** deletes any pending signup for an email.
- **Where it's called:** `AuthService.startRegistration`, to clear a leftover pending record before creating a fresh one.
- **Why it's written this way:** a new signup attempt should replace any old, unverified one, keeping one pending record per email (also enforced by the unique index).
- **Step by step:** Spring builds a delete query on `email` and removes the matching document(s).
