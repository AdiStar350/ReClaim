# UserRepository.java

## Overview
This file is a **Repository**: an interface that reads and writes one type of data in the database. A repository is where the app talks to MongoDB for `User` records (see `docs/GLOSSARY.md`). You don't write the database code — you only declare method names in a specific pattern, and Spring Data MongoDB generates the query for you at startup.

## Concepts used here
- **Repository** — an interface Spring turns into database code for one model; see `docs/GLOSSARY.md`.
- **Spring Data derived query** — Spring reads the method name (like `findByEmail`) and writes the matching query automatically.
- **`Optional`** — a container that either holds a value or is empty, used instead of returning `null`.

## Libraries and imports
- **Spring Data MongoDB** (`org.springframework.data.mongodb.repository.MongoRepository`): extending `MongoRepository<User, String>` gives this interface ready-made methods — `save`, `findById`, `findAll`, `delete`, `deleteById`, `existsById`, and more — for the `User` type whose id is a `String`. The services use several of these inherited methods (e.g. `findById`, `save`, `delete`).
- **Spring** (`org.springframework.stereotype.Repository`): `@Repository` marks this as a data-access bean Spring should manage and lets it translate database errors into Spring's exceptions.
- **Project class** (`com.example.reclaimbackend.model.User`): the model this repository stores.
- **Java standard** (`java.util.Optional`): the return type for a lookup that may find nothing.

## Functions
> These are declarations only. Spring Data generates the implementation from each method name.

### `Optional<User> findByEmail(String email)`
```java
Optional<User> findByEmail(String email);
```
- **What it does:** finds the one user with this email, or returns an empty `Optional` if none exists.
- **Where it's called:** `AuthService.login`, `AuthService.startPasswordReset`, and `AuthService.resetPassword`.
- **Why it's written this way:** returning `Optional` (instead of `null`) forces the caller to handle "not found" on purpose — for example, login turns an empty result into a `401`.
- **Step by step:**
  1. Spring reads the name `findByEmail` and builds a query "match documents where `email` equals the argument."
  2. It runs the query on the `users` collection.
  3. If a document matches, it is returned wrapped in `Optional`; otherwise the `Optional` is empty.

### `boolean existsByEmail(String email)`
```java
boolean existsByEmail(String email);
```
- **What it does:** returns `true` if some user already has this email, without loading the whole record.
- **Where it's called:** `AuthService.register`, `AuthService.startRegistration`, `AuthService.verifyRegistration`, and `UserService.updateProfile`.
- **Why it's written this way:** a boolean existence check is cheaper and clearer than fetching a user just to see if one exists; it powers the "email already registered" (`409`) checks.
- **Step by step:**
  1. Spring builds an "exists" query on `email` from the method name.
  2. It asks Mongo whether any matching document exists.
  3. It returns `true`/`false`.
