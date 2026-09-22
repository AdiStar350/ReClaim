# RegisterRequest.java

## Overview
This file is a **DTO (Data Transfer Object)** that carries sign-up fields into the API.
A DTO's only job is to hold the data for one request and control its JSON shape; see
DTO and Gson in `docs/GLOSSARY.md`. `RegisterRequest` is the body for
`POST api/auth/register`.

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each Java field to the JSON key the backend expects;
  see `docs/GLOSSARY.md`.
- **Immutable fields (`final`).** All fields are set once in the constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** The `@SerializedName`
  annotations set the JSON keys `email`, `password`, `name`, and `phoneNumber`.

## Fields
- `@SerializedName("email") private final String email` — sign-up email.
- `@SerializedName("password") private final String password` — chosen password.
- `@SerializedName("name") private final String name` — display name.
- `@SerializedName("phoneNumber") private final String phoneNumber` — phone number,
  sent as JSON key `phoneNumber`.

All four are `final`, set by the constructor
`RegisterRequest(String email, String password, String name, String phoneNumber)`.
Plain getters return each field and carry no logic.

## Where it's used
This DTO matches the `register` endpoint. Note that the Android client currently signs
users up through the OTP flow (`SendOtpRequest` → `verifyRegistrationOtp`), so
`RegisterRequest` is not constructed by app code today; it documents the direct
`POST api/auth/register` contract.
