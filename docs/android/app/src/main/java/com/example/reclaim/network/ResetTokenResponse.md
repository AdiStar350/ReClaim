# ResetTokenResponse.java

## Overview
This file is a **response DTO**: a small class that carries data back from an API call.
See DTO and Gson in `docs/GLOSSARY.md`. `ResetTokenResponse` is the reply from
`POST api/auth/forgot-password/verify-otp`. When the user's reset code is correct, the
server returns a short-lived reset token here; the app then sends that token in a
`ResetPasswordRequest` to actually change the password.

## Concepts used here
- **DTO (response side).** A carrier object for one response body; see
  `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Binds the JSON key `resetToken` to the Java field when
  parsing; see `docs/GLOSSARY.md`.
- **No explicit constructor.** Gson uses the default constructor and fills the field.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** `@SerializedName("resetToken")`
  maps the JSON `resetToken` key to the field.

## Fields
- `@SerializedName("resetToken") private String resetToken` — the short-lived token the
  app must include when setting the new password; not `final` because Gson fills it
  after construction.

The only method is a plain getter `getResetToken()` with no logic.

## Where it's used
Returned by `ReClaimApiService.verifyPasswordResetOtp`. `VerifyOtpActivity` reads
`getResetToken()` and passes it on to the reset-password step.
