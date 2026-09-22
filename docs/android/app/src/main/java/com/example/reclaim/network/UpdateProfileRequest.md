# UpdateProfileRequest.java

## Overview
This file is a **DTO (Data Transfer Object)** that carries edited profile fields. A DTO
holds one request body and fixes its JSON shape; see DTO and Gson in
`docs/GLOSSARY.md`. `UpdateProfileRequest` is the body for `PUT api/users/me`: the
user's name, email, phone, and an optional profile photo URL.

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each field to its JSON key; see `docs/GLOSSARY.md`.
- **Overloaded constructors.** Two constructors let callers include a photo URL or
  leave it out; the shorter one calls the longer one with `null`.
- **Immutable fields (`final`).** All values are set once at construction.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Sets the JSON keys `name`,
  `email`, `phoneNumber`, and `profilePictureUrl`.

## Fields
- `@SerializedName("name") private final String name` — display name.
- `@SerializedName("email") private final String email` — email.
- `@SerializedName("phoneNumber") private final String phoneNumber` — phone number.
- `@SerializedName("profilePictureUrl") private final String profilePictureUrl` — URL
  of the profile photo, or `null` when unchanged.

## Constructors
There are two:
- `UpdateProfileRequest(String name, String email, String phoneNumber)` — a
  convenience form that forwards to the full constructor with `profilePictureUrl` set to
  `null` (used when the photo is not being changed).
- `UpdateProfileRequest(String name, String email, String phoneNumber, String
  profilePictureUrl)` — the full form that sets every field.

This "one constructor calls the other" pattern avoids repeating the assignment code and
keeps a single place where all fields are set. There are no getters.

## Where it's used
Built in `EditProfileActivity` (with the four-argument constructor, including the newly
uploaded photo URL) and passed to `ReClaimApiService.updateCurrentUser`.
