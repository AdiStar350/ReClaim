# DeleteAccountRequest.java

## Overview
This file is a **DTO (Data Transfer Object)** that carries the confirmation needed to
delete an account. A DTO holds one request body and fixes its JSON shape; see DTO and
Gson in `docs/GLOSSARY.md`. `DeleteAccountRequest` is the body for `DELETE
api/users/me`; it re-sends the user's name and password so the server can confirm the
person really means to delete their account.

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each field to its JSON key; see `docs/GLOSSARY.md`.
- **Body on a DELETE.** Normally DELETE has no body, so the API method uses Retrofit's
  `@HTTP(... hasBody = true)` to attach this DTO; see the `ReClaimApiService` doc.
- **Immutable fields (`final`).** Both values are set once in the constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Sets the JSON keys `name`
  and `password`.

## Fields
- `@SerializedName("name") private final String name` — the user's name, re-typed to
  confirm.
- `@SerializedName("password") private final String password` — the user's password,
  re-typed to confirm.

Set by the constructor `DeleteAccountRequest(String name, String password)`. There are
no getters; the object is only serialized and sent. Requiring the password again is a
safety check against accidental or unauthorized deletion.

## Where it's used
Built in `ProfileSettingsFragment` when the user confirms account deletion, and passed
to `ReClaimApiService.deleteAccount`.
