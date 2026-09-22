# LoginRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `LoginRequest` is the JSON body the app sends to log in: an email and a password. It deliberately holds nothing else, so the login endpoint only sees what it needs.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Authentication** — proving who you are (logging in); see `docs/GLOSSARY.md`.
- **Validation** — `@NotBlank` and `@Email` reject bad input before our code runs; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`NotBlank`, `Email`): require both fields and check the email's shape, so an empty or malformed login is rejected before the service runs.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors and constructors. The all-args constructor is what lets `AuthServiceTest` build one in a single line (`new LoginRequest("user@example.com", "wrong-password")`).

## Fields
```java
@NotBlank(message = "Email is required")
@Email(message = "Email must be a valid email address")
private String email;

@NotBlank(message = "Password is required")
private String password;
```
- `email` — the account's email address.
- `password` — the plain password the user typed. It is compared against the stored hash and never saved anywhere.

**Annotations used and why:**
- `@NotBlank` — each field must be present and non-empty.
- `@Email` — the email must be shaped like an email address. (The password has no format rule here, so any non-empty text passes.)
- `@Getter`/`@Setter` — create getters/setters used by the JSON library and the service.
- `@NoArgsConstructor` / `@AllArgsConstructor` / `@Builder` — constructors and builder used by the JSON library, calling code, and tests.

Spring builds this object from the JSON body of `POST /api/auth/login` in `AuthController.login`, which passes it to `AuthService.login`.
