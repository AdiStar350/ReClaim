# UserService.java

## Overview
This is a **service** for the signed-in user's **profile**: read, update, save an **FCM** token, and delete the account (see `docs/GLOSSARY.md`). It returns `UserResponse` DTOs (no password) to controllers, and also exposes `getUserEntity` for other services that need the full `User` model (for example claim notifications).

## Concepts used here
- **Service** — profile logic behind `UserController`; see `docs/GLOSSARY.md`.
- **DTO** — `UserResponse` / `UpdateProfileRequest` shape the API; see `docs/GLOSSARY.md`.
- **Model / entity** — `User` is the stored document; see `docs/GLOSSARY.md`.
- **Password hashing** — delete-account checks the typed password with `PasswordEncoder`; see `docs/GLOSSARY.md`.
- **Firebase Storage** — replacing a profile picture deletes the old file; see `docs/GLOSSARY.md`.
- **FCM** — device token stored for push notifications; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring** (`@Service`, `HttpStatus`, `ResponseStatusException`, `PasswordEncoder`): service bean, API errors, password checks.
- **Lombok** (`@RequiredArgsConstructor`): constructor injection of repositories and helpers.
- **Project repositories / DTOs / model / `FirebaseStorageService`**: persistence and storage cleanup.

## Fields
- `userRepository` (`UserRepository`) — load/save/delete users.
- `itemRepository` (`ItemRepository`) — delete the user's items on account deletion.
- `claimRepository` (`ClaimRepository`) — delete claims tied to those items or made by the user.
- `passwordEncoder` (`PasswordEncoder`) — verifies password on delete.
- `firebaseStorageService` (`FirebaseStorageService`) — deletes an old profile image when replaced.

## Functions

### `private UserResponse toResponse(User user)`
```java
private UserResponse toResponse(User user) {
    return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .phoneNumber(user.getPhoneNumber())
            .profilePictureUrl(user.getProfilePictureUrl())
            .build();
}
```
- **What it does:** maps a `User` entity to a safe API DTO without password or FCM token.
- **Where it's called:** `getProfile` and `updateProfile` in this class.
- **Why it's written this way:** keeps internal fields out of JSON responses.
- **Step by step:**
  1. Copy id, email, name, phone, and profile picture URL into a builder.
  2. Return the built `UserResponse`.

### `private static String blankToNull(String value)`
```java
private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }
    return value.trim();
}
```
- **What it does:** treats blank strings as `null` after trim.
- **Where it's called:** `updateProfile` when comparing old and new profile picture URLs.
- **Why it's written this way:** empty string and null should count as the same “no picture” state.
- **Step by step:**
  1. If null or blank, return null.
  2. Otherwise return the trimmed value.

### `public UserResponse getProfile(String userId)`
```java
public UserResponse getProfile(String userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));
    return toResponse(user);
}
```
- **What it does:** loads the user and returns a `UserResponse`, or `404` if missing.
- **Where it's called:** `UserController.getCurrentUser`.
- **Why it's written this way:** controllers never see the password hash.
- **Step by step:**
  1. Find the user by id or throw 404.
  2. Convert with `toResponse`.

### `public UserResponse updateProfile(String userId, UpdateProfileRequest request)`
```java
public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));

    if (request.getEmail() != null && !request.getEmail().isBlank()) {
        if (!request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email is already registered");
        }
        user.setEmail(request.getEmail().trim());
    }

    if (request.getName() != null && !request.getName().isBlank()) {
        user.setName(request.getName().trim());
    }

    if (request.getPhoneNumber() != null) {
        user.setPhoneNumber(request.getPhoneNumber().trim());
    }

    if (request.getProfilePictureUrl() != null) {
        String url = request.getProfilePictureUrl().trim();
        String newUrl = url.isEmpty() ? null : url;
        String previousUrl = user.getProfilePictureUrl();
        if (!Objects.equals(blankToNull(previousUrl), newUrl)) {
            firebaseStorageService.deleteReplacedImage(previousUrl, newUrl);
        }
        user.setProfilePictureUrl(newUrl);
    }

    return toResponse(userRepository.save(user));
}
```
- **What it does:** updates email/name/phone/picture when provided; rejects a taken email; deletes the old Storage image when the picture URL changes.
- **Where it's called:** `UserController.updateCurrentUser`.
- **Why it's written this way:** field-by-field updates avoid wiping unset fields; storage cleanup runs before saving the new URL.
- **Step by step:**
  1. Load the user or throw 404.
  2. Optionally update email (with uniqueness check), name, phone.
  3. If picture URL is present, delete the previous Storage object when it changed, then set the new URL.
  4. Save and return `toResponse`.

### `public User getUserEntity(String userId)`
```java
public User getUserEntity(String userId) {
    return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));
}
```
- **What it does:** returns the full `User` document or throws 404.
- **Where it's called:** `updateFcmToken` and `deleteAccount` in this class; `ClaimService.submitClaim`, `notifyClaimantOfDecision`, `getPendingClaimsForOwner`, and `getContactForApprovedClaim`.
- **Why it's written this way:** other services need FCM tokens / contact fields that are not on `UserResponse`.
- **Step by step:**
  1. Find by id.
  2. Return the user or throw 404.

### `public void updateFcmToken(String userId, String fcmToken)`
```java
public void updateFcmToken(String userId, String fcmToken) {
    User user = getUserEntity(userId);
    user.setFcmToken(fcmToken);
    userRepository.save(user);
}
```
- **What it does:** stores the device push token on the user.
- **Where it's called:** `UserController.updateFcmToken`.
- **Why it's written this way:** a dedicated method keeps profile edits separate from notification setup.
- **Step by step:**
  1. Load the user via `getUserEntity`.
  2. Set `fcmToken` and save.

### `public void deleteAccount(String userId, String name, String password)`
```java
public void deleteAccount(String userId, String name, String password) {
    User user = getUserEntity(userId);

    if (name == null || !name.trim().equalsIgnoreCase(user.getName())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Name does not match account");
    }
    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Incorrect password");
    }

    itemRepository.findByOwnerId(userId).forEach(item ->
            claimRepository.findByItemId(item.getId())
                    .forEach(claimRepository::delete));
    itemRepository.findByOwnerId(userId).forEach(itemRepository::delete);
    claimRepository.findByClaimantId(userId).forEach(claimRepository::delete);
    userRepository.delete(user);
}
```
- **What it does:** verifies typed name and password, then deletes the user's items (and claims on them), the user's own claims, and the user row.
- **Where it's called:** `UserController.deleteCurrentUser`.
- **Why it's written this way:** double confirmation reduces accidental deletes; cleaning related data avoids orphaned items/claims.
- **Step by step:**
  1. Load the user.
  2. Require matching name (case-insensitive) and matching password hash.
  3. Delete claims on owned items, then owned items, then claims made by the user.
  4. Delete the user.
