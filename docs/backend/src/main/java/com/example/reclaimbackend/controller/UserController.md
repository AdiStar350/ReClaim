```java
@GetMapping("/me")
public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(userService.getProfile(userId));
}
```

What that function does: I return the signed-in user's profile without the password.
What the function is responsible for: I am GET /api/users/me.
Where it works: I am an HTTP endpoint. I call UserService.getProfile.
Why I chose to write the code of the function the way its written: I take the user id from Authentication so the caller cannot ask for someone else's profile.
More: I use Authentication.getName, which JwtAuthenticationFilter set to the user id. ResponseEntity.ok wraps UserResponse.

```java
@PutMapping("/me")
public ResponseEntity<UserResponse> updateCurrentUser(
        @Valid @RequestBody UpdateProfileRequest request,
        Authentication authentication) {
    String userId = authentication.getName();
    return ResponseEntity.ok(userService.updateProfile(userId, request));
}
```

What that function does: I update the signed-in user's profile and return the new shape.
What the function is responsible for: I am PUT /api/users/me.
Where it works: I am an HTTP endpoint. I call UserService.updateProfile.
Why I chose to write the code of the function the way its written: I pass the token user id plus the body so the service can change only that account.
More: I use @Valid UpdateProfileRequest and Authentication.getName.

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

What that function does: I save the device token for this user and return no body.
What the function is responsible for: I am PUT /api/users/me/fcm-token.
Where it works: I am an HTTP endpoint. I call UserService.updateFcmToken.
Why I chose to write the code of the function the way its written: I wrote it this way because 204 is enough; the client does not need the token echoed back.
More: I use FcmTokenRequest.getToken and ResponseEntity.noContent.

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

What that function does: I delete the signed-in account after name and password are checked.
What the function is responsible for: I am DELETE /api/users/me.
Where it works: I am an HTTP endpoint. I call UserService.deleteAccount.
Why I chose to write the code of the function the way its written: I pass name and password from the body so the service can refuse a mismatch. 204 means the account is gone.
More: I use DeleteAccountRequest getters and ResponseEntity.noContent.
