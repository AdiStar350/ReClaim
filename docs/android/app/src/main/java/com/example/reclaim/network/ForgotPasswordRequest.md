# ForgotPasswordRequest.java

## Overview
This file is a **DTO (Data Transfer Object)**: a tiny class that carries one request
body to the API. A DTO holds the data for a single call and fixes its JSON shape; see
DTO and Gson in `docs/GLOSSARY.md`. `ForgotPasswordRequest` is the body for
`POST api/auth/forgot-password/send-otp`; it carries just the email that should receive
a password-reset code.

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps the Java `email` field to the JSON key `email`; see
  `docs/GLOSSARY.md`.
- **Immutable field (`final`).** The email is set once in the constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** `@SerializedName("email")`
  sets the JSON key for the one field.

## Fields
- `@SerializedName("email") private final String email` — the address to send the
  reset code to; sent as JSON key `email`.

Set by the constructor `ForgotPasswordRequest(String email)`. The class has no getters;
it is only ever serialized and sent.

## Where it's used
Built and passed to `ReClaimApiService.sendPasswordResetOtp` from `LoginActivity` (the
forgot-password dialog), `ProfileSettingsFragment` (change-password), and
`VerifyOtpActivity` (resend in reset mode).
