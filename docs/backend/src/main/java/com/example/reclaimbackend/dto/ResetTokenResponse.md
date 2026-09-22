# ResetTokenResponse.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `ResetTokenResponse` is an **output** DTO sent back after the user's password-reset code is accepted. It carries the one-time `resetToken` the app must include in the final "set new password" request.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Reset token** — a random string that proves the code step succeeded, needed for the final reset.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate the getter (read by the JSON library), setters, both constructors, and a builder. No validation, because this DTO is only sent out.

## Fields
```java
private String resetToken;
```
- `resetToken` — the random token the server generated after the emailed code was verified. The app stores it briefly and sends it back in `ResetPasswordRequest` to finish the reset.

**Annotations used and why:**
- `@Getter`/`@Setter` — create `getResetToken()`/`setResetToken(...)`; the getter turns this into `{"resetToken": "..."}` JSON.
- `@AllArgsConstructor` — lets the controller build it in one line: `new ResetTokenResponse(resetToken)`.
- `@NoArgsConstructor` / `@Builder` — an empty constructor and builder, available for flexibility.

This object is built in `AuthController.verifyPasswordResetOtp` (using the token returned by `AuthService.verifyPasswordResetOtp`) and returned as the body of `POST /api/auth/forgot-password/verify-otp`.
