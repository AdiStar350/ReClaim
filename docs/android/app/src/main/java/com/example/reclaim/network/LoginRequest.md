# LoginRequest.java

## Overview
This file is a **DTO (Data Transfer Object)**: a small class whose only job is to
carry data into an API call. A DTO keeps the shape of what you send separate from your
database models, so the API contract is explicit. See DTO and Gson in
`docs/GLOSSARY.md`. `LoginRequest` is the JSON body for `POST api/auth/login`: it
bundles the email and password the user typed.

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps a Java field to the exact JSON key the backend
  expects; see `docs/GLOSSARY.md`.
- **Immutable fields (`final`).** The values are set once in the constructor and never
  change, which is safe for a request that is built then sent.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** `@SerializedName("email")`
  and `@SerializedName("password")` tell Gson to write these fields as the JSON keys
  `email` and `password` when Retrofit serializes the request body.

## Fields
- `@SerializedName("email") private final String email` — the user's email; sent as
  JSON key `email`.
- `@SerializedName("password") private final String password` — the user's password;
  sent as JSON key `password`.

Both fields are `final` and set by the constructor `LoginRequest(String email, String
password)`. The class also has plain getters (`getEmail`, `getPassword`) that simply
return each field; they are not broken down individually because they carry no logic.

## Where it's used
Built in `LoginActivity.attemptLogin` and in `ResetPasswordActivity` (to sign in right
after a password reset), then passed to `ReClaimApiService.login`.
