# ResendOtpRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `ResendOtpRequest` is the JSON body the app sends when a signup code didn't arrive and the user asks for a new one. It carries just the email that is waiting to be verified.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **OTP (One-Time Password)** — the 6-digit email code; this request asks the server to send a fresh one; see `docs/GLOSSARY.md`.
- **Validation** — `@NotBlank` and `@Email` reject bad input; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`Email`, `NotBlank`): require the email and check its shape.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors and constructors used by the JSON library and code.

## Fields
```java
@NotBlank(message = "Email is required")
@Email(message = "Email must be a valid email address")
private String email;
```
- `email` — the address of the pending signup that should get a new code.

**Annotations used and why:**
- `@NotBlank` — the email must be present.
- `@Email` — the email must look valid; otherwise `400 Bad Request`.
- `@Getter`/`@Setter` — create `getEmail()`/`setEmail(...)`.
- `@NoArgsConstructor` / `@AllArgsConstructor` / `@Builder` — constructors and builder for the JSON library and code.

Spring builds this object from the JSON body of `POST /api/auth/register/resend-otp` in `AuthController.resendRegistrationOtp`, which passes `getEmail()` to `AuthService.resendOtp`.
