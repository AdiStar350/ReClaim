# VerifyOtpRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `VerifyOtpRequest` is the JSON body for checking an emailed 6-digit code. It carries the email and the code, and is reused for **two** flows: finishing signup and verifying a password reset.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **OTP (One-Time Password)** — the emailed 6-digit code being checked here; see `docs/GLOSSARY.md`.
- **Validation** — annotations require the fields and force the code to be exactly 6 digits; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`Email`, `NotBlank`, `Pattern`): `@NotBlank` requires values, `@Email` checks the email shape, and `@Pattern(regexp = "\\d{6}")` requires the code to be exactly six digits.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors and constructors used by the JSON library and code.

## Fields
```java
@NotBlank(message = "Email is required")
@Email(message = "Email must be a valid email address")
private String email;

@NotBlank(message = "Verification code is required")
@Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
private String code;
```
- `email` — the account/pending signup being verified.
- `code` — the 6-digit code the user typed; must be exactly six digits (`\d{6}`).

**Annotations used and why:**
- `@NotBlank` — both fields are required.
- `@Email` — the email must look valid.
- `@Pattern(regexp = "\\d{6}")` — blocks anything that isn't exactly six digits before the service runs, so obviously-wrong codes never count against the attempt limit.
- `@Getter`/`@Setter` — accessors used by the JSON library and the service.
- `@NoArgsConstructor` / `@AllArgsConstructor` / `@Builder` — constructors and builder.

Spring builds this object from the JSON body of two endpoints: `POST /api/auth/register/verify-otp` (`AuthController.verifyRegistrationOtp` → `AuthService.verifyRegistration`) and `POST /api/auth/forgot-password/verify-otp` (`AuthController.verifyPasswordResetOtp` → `AuthService.verifyPasswordResetOtp`).
