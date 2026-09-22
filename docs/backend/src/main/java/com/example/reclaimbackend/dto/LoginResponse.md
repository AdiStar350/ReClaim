# LoginResponse.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `LoginResponse` is an **output** DTO: it is the JSON the server sends back after a successful login or signup, and it holds just the **JWT** token the app must send on later requests to prove it is logged in (see `docs/GLOSSARY.md`).

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **JWT (JSON Web Token)** — a signed string that proves the user is logged in; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate the getter (which the JSON library reads to build the response), setters, both constructors, and a builder. There is no validation because this DTO is only sent out.

## Fields
```java
public class LoginResponse { private String token; }
```
- `token` — the signed JWT for the user. The app stores it and sends it back in the `Authorization: Bearer <token>` header on future requests.

**Annotations used and why:**
- `@Getter`/`@Setter` — create `getToken()`/`setToken(...)`; the getter is what turns this object into `{"token": "..."}` JSON.
- `@AllArgsConstructor` — lets the service build it in one line: `new LoginResponse(token)`.
- `@NoArgsConstructor` / `@Builder` — an empty constructor and a builder, available for flexibility.

This object is built by `AuthService.login` and `AuthService.verifyRegistration`, and returned as the body of `POST /api/auth/login` and `POST /api/auth/register/verify-otp`.
