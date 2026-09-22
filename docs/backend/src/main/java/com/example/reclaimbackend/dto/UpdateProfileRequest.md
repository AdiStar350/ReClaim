# UpdateProfileRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `UpdateProfileRequest` is the JSON body a signed-in user sends to change their profile. Every field is optional: the service only updates a field if a value was actually sent, so the app can send just the parts that changed.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Partial update** — a request where any field may be left out, and only the provided fields are changed.
- **Validation** — `@Email` checks the email shape *if* an email is provided; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters and setters; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`jakarta.validation.constraints.Email`): if `email` is present, it must look like a valid email. There is no `@NotBlank` here, because fields are optional.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`): generate accessors and the empty constructor the JSON library needs.

## Fields
```java
private String name;

@Email(message = "Email must be valid")
private String email;

private String phoneNumber;

private String profilePictureUrl;
```
- `name` — new display name, if changing it.
- `email` — new email, if changing it; the service also checks it isn't already used by another account.
- `phoneNumber` — new phone number, if changing it.
- `profilePictureUrl` — new profile photo URL, if changing it; the service deletes the old stored photo when this differs.

**Annotations used and why:**
- `@Email` — validates the email format only when one is supplied (a missing email is allowed here).
- `@Getter`/`@Setter` — accessors; the setters let the JSON library fill the object, the getters let the service read each optional value.
- `@NoArgsConstructor` — empty constructor the JSON library uses to build the object.

Spring builds this object from the JSON body of `PUT /api/users/me` in `UserController.updateCurrentUser`, which passes it to `UserService.updateProfile`.
