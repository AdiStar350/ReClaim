# ResetPasswordRequest.java

## Overview
This file is a **DTO (Data Transfer Object)** that carries the final step of a password
reset. A DTO holds the data for one request and controls its JSON shape; see DTO and
Gson in `docs/GLOSSARY.md`. `ResetPasswordRequest` is the body for
`POST api/auth/forgot-password/reset`: the email, the reset token proving the code was
verified, and the new password.

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each field to its JSON key; see `docs/GLOSSARY.md`.
- **Reset token.** A short-lived token returned by `verifyPasswordResetOtp`; sending it
  back proves the user recently passed the OTP check.
- **Immutable fields (`final`).** All values are set once in the constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Sets the JSON keys `email`,
  `resetToken`, and `newPassword`.

## Fields
- `@SerializedName("email") private final String email` — the account email.
- `@SerializedName("resetToken") private final String resetToken` — the token from the
  verify-OTP step; proves the user is allowed to reset.
- `@SerializedName("newPassword") private final String newPassword` — the new password
  to set.

Set by the constructor
`ResetPasswordRequest(String email, String resetToken, String newPassword)`. There are
no getters; the object is only serialized and sent.

## Where it's used
Built in `ResetPasswordActivity` and passed to `ReClaimApiService.resetPassword`.
