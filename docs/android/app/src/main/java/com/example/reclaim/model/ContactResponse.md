# ContactResponse.java

## Overview
This file is a **response DTO**: a small class that carries data back from an API call.
See DTO and Gson in `docs/GLOSSARY.md`. `ContactResponse` holds the contact details
(name, email, phone) that one party is allowed to see about the other once a claim is
approved or when a finder needs to reach a lost item's owner. It is a *response-only*
shape — the app never sends it, only receives it.

## Concepts used here
- **DTO (response side).** A carrier object for one response body; see
  `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps each JSON key to its Java field when parsing; see
  `docs/GLOSSARY.md`.
- **No explicit constructor.** Gson uses the default constructor and fills the fields.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** Tags each field with its
  JSON key (`name`, `email`, `phoneNumber`).

## Fields
- `@SerializedName("name") private String name` — the other person's name.
- `@SerializedName("email") private String email` — their email.
- `@SerializedName("phoneNumber") private String phoneNumber` — their phone number.

Fields are not `final` because Gson fills them after construction. The only methods are
plain getters (`getName`, `getEmail`, `getPhoneNumber`) with no logic. Contact details
are exposed only through this narrow shape (and only by endpoints that check
permission), so the full `User` record is never leaked.

## Where it's used
Returned by `getLostItemOwnerContact` and `getClaimContact`. Shown in the contact
dialogs from `ItemDetailsActivity`, `HomeFragment`, `PendingClaimsSheet`, and
`ReportHistoryActivity`.
