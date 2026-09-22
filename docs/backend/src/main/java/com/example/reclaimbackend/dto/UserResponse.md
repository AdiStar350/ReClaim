# UserResponse.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `UserResponse` is an **output** DTO: it is the safe, public view of a `User`. It has every profile field **except** the password (and other internal data), so the server never leaks the password hash when it returns a user.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **Model vs DTO** — the `User` **model** is the full database record (including the password hash); this DTO is the trimmed version safe to send to clients; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors, constructors, and the `builder()` the service uses to copy fields from a `User`. No validation, because this DTO is only sent out.

## Fields
```java
private String id;
private String email;
private String name;
private String phoneNumber;
private String profilePictureUrl;
```
- `id` — the user's id.
- `email` — the user's email address.
- `name` — the user's display name.
- `phoneNumber` — the user's phone number.
- `profilePictureUrl` — link to the user's profile photo, if any.

Note there is **no** `password` field — that is the whole point of having a separate response type.

**Annotations used and why:**
- `@Getter`/`@Setter` — create getters (read by the JSON library) and setters.
- `@Builder` — lets `UserService.toResponse` build one field by field from a `User`.
- `@NoArgsConstructor` / `@AllArgsConstructor` — empty and all-fields constructors.

This object is built by `UserService.toResponse` and returned from `GET /api/users/me` and `PUT /api/users/me` (via `UserController.getCurrentUser` and `updateCurrentUser`).
