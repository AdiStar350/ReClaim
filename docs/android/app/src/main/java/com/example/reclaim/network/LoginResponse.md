# LoginResponse.java

## Overview
This file is a **response DTO**: a small class that carries data *back* from an API
call. Where a request DTO describes what the app sends, a response DTO describes what
the server returns. See DTO and Gson in `docs/GLOSSARY.md`. `LoginResponse` is the
reply from `POST api/auth/login` (and the OTP sign-in endpoints); it holds the login
token the app must save.

## Concepts used here
- **DTO (response side).** A carrier object for one response body; see
  `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps the JSON key `token` to the Java `token` field when
  Gson parses the reply; see `docs/GLOSSARY.md`.
- **No-argument constructor for Gson.** Gson creates the object with an empty
  constructor and then fills the fields, so response DTOs provide one.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** `@SerializedName("token")`
  binds the JSON `token` key to the `token` field during parsing.

## Fields
- `@SerializedName("token") private String token` — the login token (a JWT) returned by
  the server; not `final` because Gson fills it after construction, and a setter also
  exists.

The class provides an empty constructor `LoginResponse()` (used by Gson), a
convenience constructor `LoginResponse(String token)`, a getter `getToken()`, and a
setter `setToken(...)`. These are plain accessors with no logic, so they are not
broken down individually.

## Where it's used
Returned by `ReClaimApiService.login`, `verifyRegistrationOtp`, etc. Callers such as
`LoginActivity` and `VerifyOtpActivity` read `getToken()` and hand it to
`TokenManager.saveToken`.
