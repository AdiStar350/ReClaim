# User.java

## Overview
This file is a **model class**: a plain Java object that mirrors one user record from
the backend. One `User` object equals one user on the server (minus secret fields like
the password hash, which the API never sends). See model and Gson in
`docs/GLOSSARY.md`. Gson fills a `User` when parsing the profile endpoints, and the app
reads its fields to show the profile.

## Concepts used here
- **Model.** A class that mirrors one stored record; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each Java field to its JSON key when parsing; see
  `docs/GLOSSARY.md`.
- **Getters and setters.** Gson writes the fields and screens read them, so the class
  exposes plain accessors and a no-argument constructor.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Tags each field with its
  JSON key.

## Fields
Each field is `@SerializedName`-tagged with the matching JSON key.
- `id` — the user's unique id.
- `email` — the user's email.
- `name` — display name.
- `phoneNumber` — phone number.
- `profilePictureUrl` — URL of the profile photo.

Note there is no password field: the backend never returns the password (only a hashed
version is stored server-side), so the client model has no place to hold it.

## Constructors and accessors
- No-argument constructor `User()` — used by Gson.
- Convenience constructor `User(id, email, name, phoneNumber)` — sets the main fields.
- Plain getters and setters for each field, with no logic, so they are not listed one
  by one per the DTO/model style.

## Where it's used
Parsed from `getCurrentUser`, `updateCurrentUser`, and `register`. Shown in
`HomeFragment`, `EditProfileActivity`, and `ProfileSettingsFragment`.
