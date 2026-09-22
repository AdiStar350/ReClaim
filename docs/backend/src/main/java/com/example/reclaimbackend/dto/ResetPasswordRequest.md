# ResetPasswordRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `ResetPasswordRequest` is the JSON body for the final step of a password reset: it carries the email, the short-lived reset token the server handed out after the code was checked, and the new password to set.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Reset token** — a random string the server gives out once the emailed code is accepted, proving the user finished the code step.
- **Validation** — annotations enforce required fields, email shape, and password length; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`Email`, `NotBlank`, `Size`): require the fields, check the email shape, and require the new password to be at least 6 characters.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors and constructors used by the JSON library and code.

## Fields
```java
@NotBlank(message = "Email is required")
@Email(message = "Email must be a valid email address")
private String email;

@NotBlank(message = "Reset token is required")
private String resetToken;

@NotBlank(message = "Password is required")
@Size(min = 6, message = "Password must be at least 6 characters")
private String newPassword;
```
- `email` — the account whose password is being changed.
- `resetToken` — the token from the verify step; the server checks it matches and hasn't expired before allowing the change.
- `newPassword` — the new password (min 6 characters); the service hashes it before saving.

**Annotations used and why:**
- `@NotBlank` — all three fields must be present.
- `@Email` — the email must look valid.
- `@Size(min = 6)` — the new password must be long enough.
- `@Getter`/`@Setter` — accessors used by the JSON library and the service.
- `@NoArgsConstructor` / `@AllArgsConstructor` / `@Builder` — constructors and builder.

Spring builds this object from the JSON body of `POST /api/auth/forgot-password/reset` in `AuthController.resetPassword`, which passes it to `AuthService.resetPassword`.
