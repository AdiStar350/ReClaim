# RegisterRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `RegisterRequest` is the JSON body for the direct "create account" endpoint (the path that makes a user immediately, without the email-code step). It carries the email, password, name, and optional phone number.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Validation** — annotations enforce required fields, email shape, and a minimum password length; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`Email`, `NotBlank`, `Size`): `@NotBlank` requires a value; `@Email` checks the email shape; `@Size(min = 6)` requires the password to be at least 6 characters.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors and constructors so the JSON library can build the object, plus a builder for tests.

## Fields
```java
@NotBlank(message = "Email is required")
@Email(message = "Email must be a valid email address")
private String email;

@NotBlank(message = "Password is required")
@Size(min = 6, message = "Password must be at least 6 characters")
private String password;

@NotBlank(message = "Name is required")
private String name;

private String phoneNumber;
```
- `email` — the new account's email (must be unique; the service rejects duplicates with `409 Conflict`).
- `password` — the chosen password (min 6 characters); the service hashes it before saving.
- `name` — the user's display name.
- `phoneNumber` — optional phone number; it has no validation, so it may be left out.

**Annotations used and why:**
- `@NotBlank` — email, password, and name must be present and non-empty.
- `@Email` — the email must look like an email.
- `@Size(min = 6)` — enforces a minimum password length before the service runs.
- `@Getter`/`@Setter` — accessors used by the JSON library and the service.
- `@NoArgsConstructor` / `@AllArgsConstructor` / `@Builder` — constructors and builder for the JSON library and code.

Spring builds this object from the JSON body of `POST /api/auth/register` in `AuthController.register`, which passes it to `AuthService.register`.
