# ClaimRequest.java

## Overview
This file is a **DTO (Data Transfer Object)**: a small class whose only job is to carry data between the phone app and the backend. `ClaimRequest` is the shape of the JSON body the app sends when a user wants to claim an item ("this lost/found thing is mine"). It holds just the two pieces of information the user types in; it is not stored in the database. For the general meaning of DTO see `docs/GLOSSARY.md`.

## Concepts used here
- **DTO** — a class used only to move data in/out of the API, kept separate from the database model; see `docs/GLOSSARY.md`.
- **Validation** — the `@NotBlank` tags make Spring reject an empty request before our code runs; see `docs/GLOSSARY.md`.
- **Lombok** — a library that writes getters/setters/constructors for us so we don't type them; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`jakarta.validation.constraints.NotBlank`): `@NotBlank` marks a text field that must be present and not empty/whitespace. Spring checks it because the controller marks the body `@Valid`.
- **Lombok** (`lombok.Getter`, `lombok.Setter`, `lombok.NoArgsConstructor`): generate the getter methods, setter methods, and a no-argument constructor at compile time. Spring needs a no-arg constructor plus setters to build this object from incoming JSON.

## Fields
```java
@NotBlank(message = "Item ID is required")
private String itemId;

@NotBlank(message = "Validation answer is required")
private String validationAnswer;
```
- `itemId` — the id of the item being claimed. The claimant themselves is **not** in this body; the server reads the claimant's id from the login token instead, so a user cannot pretend to be someone else.
- `validationAnswer` — the claimant's answer to the reporter's hidden "prove it's yours" question.

**Annotations used and why:**
- `@Getter` / `@Setter` (Lombok) — create `getItemId()`, `getValidationAnswer()`, and matching setters. The controller and service read these; Spring uses the setters when turning JSON into this object.
- `@NoArgsConstructor` (Lombok) — creates an empty `ClaimRequest()`. The JSON library needs this to make a blank object and then fill it.
- `@NotBlank` (Jakarta) — rejects a missing or blank value with the given message, returning `400 Bad Request` before the service runs.

This object is created by Spring from the JSON body of `POST /api/claims` in `ClaimController.submitClaim`, then passed to `ClaimService.submitClaim`.
