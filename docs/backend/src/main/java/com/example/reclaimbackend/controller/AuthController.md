```java
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    try {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    } catch (ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
    }
}
```

What that function does: I check email and password, then return a token or an error body.
What the function is responsible for: I am POST /api/auth/login.
Where it works: I am an HTTP endpoint. I call AuthService.login.
Why I chose to write the code of the function the way its written: I catch ResponseStatusException so the client gets JSON {"error": ...} instead of Spring's default page.
More: I use @Valid on LoginRequest, ResponseEntity, HttpStatus from the exception, and Map.of for the error body.

```java
@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    try {
        User savedUser = authService.register(request);
        // Do not send the password back
        savedUser.setPassword(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    } catch (ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
    }
}
```

What that function does: I create an account and return the user with the password cleared.
What the function is responsible for: I am POST /api/auth/register.
Where it works: I am an HTTP endpoint. I call AuthService.register.
Why I chose to write the code of the function the way its written: I set password to null before the reply so the hash never goes out. 201 marks a new user.
More: I use HttpStatus.CREATED, User.setPassword, and the same error Map as login.

```java
@PostMapping("/register/send-otp")
public ResponseEntity<?> sendRegistrationOtp(@Valid @RequestBody SendOtpRequest request) {
    try {
        authService.startRegistration(request);
        return ResponseEntity.ok(Map.of("message", "Verification code sent"));
    } catch (ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
    }
}
```

What that function does: I save signup details and ask the service to email a 6-digit code.
What the function is responsible for: I am POST /api/auth/register/send-otp.
Where it works: I am an HTTP endpoint. I call AuthService.startRegistration.
Why I chose to write the code of the function the way its written: I return a short message, not the code, so the code only goes to email or the log.
More: I use SendOtpRequest, Map.of, and AuthService.

```java
@PostMapping("/register/verify-otp")
public ResponseEntity<?> verifyRegistrationOtp(@Valid @RequestBody VerifyOtpRequest request) {
    try {
        return ResponseEntity.ok(authService.verifyRegistration(request));
    } catch (ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
    }
}
```

What that function does: I check the signup code and return a login token.
What the function is responsible for: I am POST /api/auth/register/verify-otp.
Where it works: I am an HTTP endpoint. I call AuthService.verifyRegistration.
Why I chose to write the code of the function the way its written: I pass the request straight through and reuse the same error mapping as the other auth routes.
More: I use VerifyOtpRequest and LoginResponse from the service.

```java
@PostMapping("/register/resend-otp")
public ResponseEntity<?> resendRegistrationOtp(@Valid @RequestBody ResendOtpRequest request) {
    try {
        authService.resendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Verification code sent"));
    } catch (ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
    }
}
```

What that function does: I send a new signup code to the given email.
What the function is responsible for: I am POST /api/auth/register/resend-otp.
Where it works: I am an HTTP endpoint. I call AuthService.resendOtp.
Why I chose to write the code of the function the way its written: I only pass email because a resend does not need the rest of the signup fields again.
More: I use ResendOtpRequest.getEmail and Map.of for the message.

```java
@PostMapping("/forgot-password/send-otp")
public ResponseEntity<?> sendPasswordResetOtp(
        @Valid @RequestBody ForgotPasswordRequest request) {
    try {
        authService.startPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "A reset code has been sent"));
    } catch (ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
    }
}
```

What that function does: I email a reset code, or return the service error if the email is unknown.
What the function is responsible for: I am POST /api/auth/forgot-password/send-otp.
Where it works: I am an HTTP endpoint. I call AuthService.startPasswordReset.
Why I chose to write the code of the function the way its written: I pass only email. The service returns 404 for an unknown address.
More: I use ForgotPasswordRequest.getEmail and Map.of.

```java
@PostMapping("/forgot-password/verify-otp")
public ResponseEntity<?> verifyPasswordResetOtp(
        @Valid @RequestBody VerifyOtpRequest request) {
    try {
        String resetToken = authService.verifyPasswordResetOtp(request);
        return ResponseEntity.ok(new ResetTokenResponse(resetToken));
    } catch (ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
    }
}
```

What that function does: I check the reset code and return a reset token.
What the function is responsible for: I am POST /api/auth/forgot-password/verify-otp.
Where it works: I am an HTTP endpoint. I call AuthService.verifyPasswordResetOtp.
Why I chose to write the code of the function the way its written: I wrap the string in ResetTokenResponse so the client gets JSON, not a raw string.
More: I reuse VerifyOtpRequest and new ResetTokenResponse(resetToken).

```java
@PostMapping("/forgot-password/reset")
public ResponseEntity<?> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request) {
    try {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    } catch (ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
    }
}
```

What that function does: I set the new password after the reset token is checked.
What the function is responsible for: I am POST /api/auth/forgot-password/reset.
Where it works: I am an HTTP endpoint. I call AuthService.resetPassword.
Why I chose to write the code of the function the way its written: I return a message on success and the same error Map on failure as the other auth routes.
More: I use ResetPasswordRequest, AuthService.resetPassword, and Map.of.
