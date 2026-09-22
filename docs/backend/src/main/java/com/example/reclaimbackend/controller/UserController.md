# UserController.java

## Overview
This is a **controller** for the signed-in user's **profile** under `/api/users` (see `docs/GLOSSARY.md`). Every route needs a JWT. The controller never takes a user id from the URL; it always uses `Authentication.getName()` and calls `UserService` for read/update profile, FCM token save, and account deletion.

## Concepts used here
- **Controller / endpoint / REST** — profile HTTP doors; see `docs/GLOSSARY.md`.
- **Authentication** — identity from the JWT via Spring Security; see `docs/GLOSSARY.md`.
- **DTO** — `UserResponse`, `UpdateProfileRequest`, `FcmTokenRequest`, `DeleteAccountRequest`; see `docs/GLOSSARY.md`.
- **Firebase / FCM** — the FCM token endpoint stores the device token for pushes; see `docs/GLOSSARY.md`.
- **Validation** — `@Valid` on bodies; see `docs/GLOSSARY.md`.
- **Dependency injection** — `UserService` is injected; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Web** (mappings, `ResponseEntity`, `@RequestBody`): HTTP routing and responses.
- **Spring Security** (`Authentication`): signed-in user id.
- **Jakarta Validation** (`@Valid`): validates request DTOs.
- **Lombok** (`@RequiredArgsConstructor`): injects `userService`.
- **Project DTOs / service**: profile shapes and `UserService`.

## Fields
- `userService` (`UserService`) — profile, FCM token, and account-deletion logic.

## Functions

### `public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication)`
```java
@GetMapping("/me")
public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(userService.getProfile(userId));
}
```
- **What it does:** returns the current user's safe profile DTO (no password).
- **Where it's called:** by Spring MVC on `GET /api/users/me`.
- **Why it's written this way:** `/me` plus token identity avoids guessing other users' ids in the path.
- **Step by step:**
  1. Read user id from authentication.
  2. Call `userService.getProfile`.
  3. Return `UserResponse`.

### `public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateProfileRequest request, Authentication authentication)`
```java
@PutMapping("/me")
public ResponseEntity<UserResponse> updateCurrentUser(
        @Valid @RequestBody UpdateProfileRequest request,
        Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(userService.updateProfile(userId, request));
}
```
- **What it does:** updates name/phone (and related fields) for the signed-in user.
- **Where it's called:** by Spring MVC on `PUT /api/users/me`.
- **Why it's written this way:** validated DTO + token id keeps updates scoped to the caller.
- **Step by step:**
  1. Validate `UpdateProfileRequest`.
  2. Call `userService.updateProfile`.
  3. Return the updated `UserResponse`.

### `public ResponseEntity<Void> updateFcmToken(@Valid @RequestBody FcmTokenRequest request, Authentication authentication)`
```java
@PutMapping("/me/fcm-token")
public ResponseEntity<Void> updateFcmToken(
        @Valid @RequestBody FcmTokenRequest request,
        Authentication authentication) {
    String userId = authentication.getName();
    userService.updateFcmToken(userId, request.getToken());
    return ResponseEntity.noContent().build();
}
```
- **What it does:** stores the device's FCM token so push notifications can reach this user.
- **Where it's called:** by Spring MVC on `PUT /api/users/me/fcm-token`.
- **Why it's written this way:** returns `204` because the client only needs success/failure, not a body.
- **Step by step:**
  1. Validate `FcmTokenRequest`.
  2. Call `userService.updateFcmToken`.
  3. Return `204 No Content`.

### `public ResponseEntity<Void> deleteCurrentUser(@Valid @RequestBody DeleteAccountRequest request, Authentication authentication)`
```java
@DeleteMapping("/me")
public ResponseEntity<Void> deleteCurrentUser(
        @Valid @RequestBody DeleteAccountRequest request,
        Authentication authentication) {
    String userId = authentication.getName();
    userService.deleteAccount(userId, request.getName(), request.getPassword());
    return ResponseEntity.noContent().build();
}
```
- **What it does:** deletes the account after the service checks the typed name and password.
- **Where it's called:** by Spring MVC on `DELETE /api/users/me`.
- **Why it's written this way:** requiring name + password in the body makes accidental deletes harder.
- **Step by step:**
  1. Validate `DeleteAccountRequest`.
  2. Call `userService.deleteAccount` with user id, name, and password.
  3. Return `204 No Content`.
