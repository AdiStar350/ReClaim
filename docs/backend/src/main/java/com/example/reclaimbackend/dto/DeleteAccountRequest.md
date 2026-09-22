# DeleteAccountRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `DeleteAccountRequest` is the JSON body a user sends when they want to permanently delete their account. It asks for the account's name and password again so the server can re-confirm it is really them before erasing everything.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Validation** — `@NotBlank` rejects empty fields before our code runs; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters and setters; see `docs/GLOSSARY.md`.
- **Re-authentication** — asking for the password again before a dangerous, irreversible action.

## Libraries and imports
- **Jakarta Bean Validation** (`jakarta.validation.constraints.NotBlank`): both fields must be present and non-empty.
- **Lombok** (`Getter`, `Setter`): generate accessors. There is no `@NoArgsConstructor` written, but Lombok/JSON still work because Java supplies a default no-arg constructor when no other constructor is declared.

## Fields
```java
@NotBlank
private String name;

@NotBlank
private String password;
```
- `name` — the name on the account, retyped to confirm identity.
- `password` — the account password, retyped to prove it is really the owner asking.

**Annotations used and why:**
- `@NotBlank` — neither value may be missing or blank; a blank value returns `400 Bad Request`.
- `@Getter`/`@Setter` — create `getName()`, `getPassword()`, and their setters. The setters let the JSON library fill the object; the getters let the service read it.

Spring builds this object from the JSON body of `DELETE /api/users/me` in `UserController.deleteCurrentUser`, which passes `getName()` and `getPassword()` to `UserService.deleteAccount`. The service checks the name matches and the password hash matches before deleting the user and their data.
