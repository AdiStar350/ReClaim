# ForgotPasswordRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `ForgotPasswordRequest` is the JSON body the app sends to start a password reset. It carries just the email address that should receive a reset code.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **OTP (One-Time Password)** — a short 6-digit code emailed to prove the person owns the email; this request kicks off sending one; see `docs/GLOSSARY.md`.
- **Validation** — `@NotBlank` and `@Email` reject bad input before our code runs; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`Email`, `NotBlank`): `@NotBlank` requires a value; `@Email` requires it to look like a valid email address.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors and constructors so the JSON library can build the object, plus a builder for tests.

## Fields
```java
@NotBlank(message = "Email is required")
@Email(message = "Email must be a valid email address")
private String email;
```
- `email` — the address of the account that forgot its password; the reset code is emailed here.

**Annotations used and why:**
- `@NotBlank` — the email must be present and non-empty.
- `@Email` — the value must be shaped like an email; a bad value returns `400 Bad Request`.
- `@Getter`/`@Setter` — create `getEmail()`/`setEmail(...)`.
- `@NoArgsConstructor` / `@AllArgsConstructor` / `@Builder` — constructors and builder used by the JSON library and calling code.

Spring builds this object from the JSON body of `POST /api/auth/forgot-password/send-otp` in `AuthController.sendPasswordResetOtp`, which passes `getEmail()` to `AuthService.startPasswordReset`.
