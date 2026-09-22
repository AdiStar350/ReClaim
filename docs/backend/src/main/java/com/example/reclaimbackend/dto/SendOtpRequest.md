# SendOtpRequest.java

## Overview
This is a **DTO (Data Transfer Object)** — a small class used only to carry data in or out of the API, separate from the database model (see `docs/GLOSSARY.md`). `SendOtpRequest` is the JSON body for the first step of the code-based signup: the app sends all the new-account details, and the server holds them and emails a 6-digit code. The real `User` is not created until the code is verified.

## Concepts used here
- **DTO** — a data-carrier class for the API; see `docs/GLOSSARY.md`.
- **OTP (One-Time Password)** — the emailed 6-digit signup code; see `docs/GLOSSARY.md`.
- **Validation** — annotations enforce required fields, email shape, and password length; see `docs/GLOSSARY.md`.
- **Lombok** — generates getters, setters, constructors, and a builder; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Jakarta Bean Validation** (`Email`, `NotBlank`, `Size`): require the key fields, check the email shape, and require a password of at least 6 characters.
- **Lombok** (`Getter`, `Setter`, `NoArgsConstructor`, `AllArgsConstructor`, `Builder`): generate accessors and constructors used by the JSON library and code.

## Fields
```java
@NotBlank(message = "Email is required")
@Email(message = "Email must be a valid email address")
private String email;

@NotBlank(message = "Password is required")
@Size(min = 6, message = "Password must be at least 6 characters")
private String password;

@NotBlank(message = "Name is required")
private String name;

private String phoneNumber;

private String profilePictureUrl;
```
- `email` — the new account's email; the code is sent here.
- `password` — the chosen password (min 6 characters); it is hashed and stored on the pending record, not kept in plain text.
- `name` — the user's display name.
- `phoneNumber` — optional phone number.
- `profilePictureUrl` — optional link to a profile photo already uploaded to storage.

**Annotations used and why:**
- `@NotBlank` — email, password, and name are required.
- `@Email` — the email must look valid.
- `@Size(min = 6)` — enforces a minimum password length.
- `@Getter`/`@Setter` — accessors used by the JSON library and the service.
- `@NoArgsConstructor` / `@AllArgsConstructor` / `@Builder` — constructors and builder.

Spring builds this object from the JSON body of `POST /api/auth/register/send-otp` in `AuthController.sendRegistrationOtp`, which passes it to `AuthService.startRegistration`.
