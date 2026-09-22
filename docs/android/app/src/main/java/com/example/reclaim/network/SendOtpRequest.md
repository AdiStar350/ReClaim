# SendOtpRequest.java

## Overview
This file is a **DTO (Data Transfer Object)** that carries all the sign-up fields when
requesting a registration code. A DTO holds the data for one request and fixes its
JSON shape; see DTO, Gson, and OTP in `docs/GLOSSARY.md`. `SendOtpRequest` is the body
for `POST api/auth/register/send-otp`. The account is not created yet — the server
emails a one-time code, and the account is made only after the code is verified.

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **OTP (One-Time Password).** The 6-digit code emailed to confirm the address; see
  `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each field to its JSON key; see `docs/GLOSSARY.md`.
- **Immutable fields (`final`).** All values are set once in the constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Sets the JSON keys `email`,
  `password`, `name`, `phoneNumber`, and `profilePictureUrl`.

## Fields
- `@SerializedName("email") private final String email` — sign-up email (also where the
  code is sent).
- `@SerializedName("password") private final String password` — chosen password.
- `@SerializedName("name") private final String name` — display name.
- `@SerializedName("phoneNumber") private final String phoneNumber` — phone number.
- `@SerializedName("profilePictureUrl") private final String profilePictureUrl` —
  optional URL of an already-uploaded profile photo.

All are `final`, set by the constructor
`SendOtpRequest(String email, String password, String name, String phoneNumber,
String profilePictureUrl)`. The server holds these until the code is verified, so all
the sign-up data travels together. There are no getters.

## Where it's used
Built in `RegisterActivity` and passed to `ReClaimApiService.sendRegistrationOtp`.
