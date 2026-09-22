# VerifyOtpRequest.java

## Overview
This file is a **DTO (Data Transfer Object)** that carries an email plus the 6-digit
code the user typed, so the server can check it. A DTO holds one request body and fixes
its JSON shape; see DTO, Gson, and OTP in `docs/GLOSSARY.md`. The same DTO is reused for
two endpoints: `POST api/auth/register/verify-otp` (sign-up) and
`POST api/auth/forgot-password/verify-otp` (password reset).

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **OTP.** The one-time code being verified; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each field to its JSON key; see `docs/GLOSSARY.md`.
- **Immutable fields (`final`).** Both values are set once in the constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Sets the JSON keys `email`
  and `code`.

## Fields
- `@SerializedName("email") private final String email` — the account email.
- `@SerializedName("code") private final String code` — the 6-digit code the user
  entered.

Set by the constructor `VerifyOtpRequest(String email, String code)`. There are no
getters; the object is only serialized and sent. One DTO serving both flows works
because both endpoints need exactly email + code.

## Where it's used
Built in `VerifyOtpActivity` and passed to either
`ReClaimApiService.verifyRegistrationOtp` (sign-up) or `verifyPasswordResetOtp`
(reset), depending on the screen's mode.
