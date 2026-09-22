# ResendOtpRequest.java

## Overview
This file is a **DTO (Data Transfer Object)**: a tiny class carrying one request body.
A DTO holds the data for a single call and fixes its JSON shape; see DTO, Gson, and OTP
in `docs/GLOSSARY.md`. `ResendOtpRequest` is the body for
`POST api/auth/register/resend-otp`; it carries just the email that should receive a
fresh sign-up code.

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **OTP.** The one-time sign-up code; this request asks the server to send a new one;
  see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps the `email` field to the JSON key `email`; see
  `docs/GLOSSARY.md`.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** `@SerializedName("email")`
  sets the JSON key.

## Fields
- `@SerializedName("email") private final String email` — the address to resend the
  code to; sent as JSON key `email`.

Set by the constructor `ResendOtpRequest(String email)`. There are no getters; the
object is only serialized and sent.

## Where it's used
Built in `VerifyOtpActivity` (sign-up mode, "resend" action) and passed to
`ReClaimApiService.resendRegistrationOtp`.
