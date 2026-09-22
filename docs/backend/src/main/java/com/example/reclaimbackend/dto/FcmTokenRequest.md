# FcmTokenRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `FcmTokenRequest` is the JSON body the app sends to register the phone's push-notification address. **FCM** (Firebase Cloud Messaging) gives each installed app a device token; the server saves it so it can later send that phone push notifications (see `docs/GLOSSARY.md`).

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **FCM token** — the address of one phone for push notifications; see `docs/GLOSSARY.md`.
- **Validation** — `@NotBlank` requires the token to be present; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, and constructors; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`jakarta.validation.constraints.NotBlank`): the token must be present and non-empty.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`): generate accessors and both constructors so the JSON library can build the object.

## Fields
```java
@NotBlank(message = "FCM token is required")
private String token;
```
- `token` — the Firebase Cloud Messaging device token for this user's phone. The server stores it on the `User` so push notifications can be sent to that device.

**Annotations used and why:**
- `@NotBlank` — the token can't be missing or blank; otherwise `400 Bad Request`.
- `@Getter`/`@Setter` — create `getToken()`/`setToken(...)`.
- `@NoArgsConstructor` / `@AllArgsConstructor` — empty and all-fields constructors used by the JSON library and by calling code.

Spring builds this object from the JSON body of `PUT /api/users/me/fcm-token` in `UserController.updateFcmToken`, which passes `getToken()` to `UserService.updateFcmToken` to save on the user.
