```java
public UserResponse getProfile(String userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));
    return toResponse(user);
}
```

What that function does: I load a user and return the profile without the password.
What the function is responsible for: I am the profile read.
Where it works: I am called by UserController.getCurrentUser from GET /api/users/me.
Why I chose to write the code of the function the way its written: I go through toResponse so the hash never leaves this class.
More: I use userRepository.findById and ResponseStatusException 404.

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

What that function does: I change the fields that were sent, then return the updated profile.
What the function is responsible for: I am the profile update. I also delete an old picture when the URL changes.
Where it works: I am called by UserController.updateCurrentUser from PUT /api/users/me.
Why I chose to write the code of the function the way its written: I skip blank name and email so a client can send only some fields. I trim values. I check a new email is free.
More: I use Objects.equals, blankToNull, FirebaseStorageService.deleteReplacedImage, and existsByEmail.

```java
public User getUserEntity(String userId) {
    return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));
}
```

What that function does: I load the full User document or throw 404.
What the function is responsible for: I am the shared user lookup for other services that need the entity, not UserResponse.
Where it works: I am called by updateFcmToken and deleteAccount. ClaimService.submitClaim, notifyClaimantOfDecision, getPendingClaimsForOwner, and getContactForApprovedClaim call me. ClaimServiceTest mocks me.
Why I chose to write the code of the function the way its written: I wrote it this way because one lookup with 404 keeps those callers short.
More: I use userRepository.findById.

```java
public void updateFcmToken(String userId, String fcmToken) {
    User user = getUserEntity(userId);
    user.setFcmToken(fcmToken);
    userRepository.save(user);
}
```

What that function does: I save the device token used to send this user notifications.
What the function is responsible for: I store fcmToken on the user.
Where it works: I am called by UserController.updateFcmToken from PUT /api/users/me/fcm-token.
Why I chose to write the code of the function the way its written: I reuse getUserEntity so a missing user is 404. I do not return the token.
More: I use User.setFcmToken and userRepository.save. PushNotificationService later reads this field.

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

What that function does: I check name and password, then delete the user's items, claims, and account.
What the function is responsible for: I remove the account and the data tied to it.
Where it works: I am called by UserController.deleteCurrentUser from DELETE /api/users/me.
Why I chose to write the code of the function the way its written: I require name and password so a stolen token alone is not enough. I delete claims on owned items first so leftover claims are less likely.
More: I use PasswordEncoder.matches, findByOwnerId, findByItemId, findByClaimantId, and 403 FORBIDDEN.

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

What that function does: I copy public profile fields onto UserResponse and skip password and fcmToken.
What the function is responsible for: I am the safe reply shape for profile reads and updates.
Where it works: I am called by getProfile and updateProfile. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because a builder copy is clearer than sending the User entity and hoping Jackson hides the hash.
More: I use UserResponse.builder.

```java
private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }
    return value.trim();
}
```

What that function does: I turn null or blank strings into null, otherwise I trim.
What the function is responsible for: I compare old and new picture URLs without treating blank as a real URL.
Where it works: I am called by updateProfile. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because static and small so the URL change check stays readable.
More: I use String.isBlank and trim. No extra libraries.
