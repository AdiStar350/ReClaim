# AuthController.java

## Overview
This is a **controller**: the HTTP front desk for login, signup OTP, and forgot-password flows (see `docs/GLOSSARY.md`). All routes live under `/api/auth` and are public (no JWT required). Each method validates the request body, calls `AuthService`, and turns success or `ResponseStatusException` into an HTTP response with JSON.

## Concepts used here
- **Controller / endpoint / REST** — maps HTTP methods and paths to Java methods; see `docs/GLOSSARY.md`.
- **DTO** — request/response shapes like `LoginRequest` and `LoginResponse`; see `docs/GLOSSARY.md`.
- **Validation** — `@Valid` rejects bad bodies before the method runs; see `docs/GLOSSARY.md`.
- **JWT** — returned after login or successful registration verify; see `docs/GLOSSARY.md`.
- **OTP** — emailed codes for signup and password reset; see `docs/GLOSSARY.md`.
- **Dependency injection** — Spring injects `AuthService` via the Lombok constructor; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Web** (`@RestController`, `@RequestMapping`, `@PostMapping`, `@RequestBody`, `ResponseEntity`, `HttpStatus`, `ResponseStatusException`): defines routes and builds HTTP responses.
- **Jakarta Validation** (`@Valid`): runs DTO validation annotations.
- **Lombok** (`@RequiredArgsConstructor`): constructor injection for `authService`.
- **Project DTOs / model / service**: request bodies, `LoginResponse`/`ResetTokenResponse`, `User`, and `AuthService` for real logic.

## Fields
- `authService` (`AuthService`) — does login, registration OTP, and password-reset work.

## Functions

### `public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request)`
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
- **What it does:** checks email/password via the service and returns a JWT (and user info) on success, or an error JSON with the service's status.
- **Where it's called:** by Spring MVC on `POST /api/auth/login` (Android `ReClaimApiService` login call).
- **Why it's written this way:** catching `ResponseStatusException` turns service failures into a consistent `{"error": "..."}` body instead of a raw stack trace.
- **Step by step:**
  1. Validate the body as a `LoginRequest`.
  2. Call `authService.login`.
  3. On success, return `200` with `LoginResponse`.
  4. On `ResponseStatusException`, return that status with an `error` map.

### `public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request)`
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
- **What it does:** creates a user directly (legacy/direct register path), clears the password field before responding, and returns `201`.
- **Where it's called:** by Spring MVC on `POST /api/auth/register`.
- **Why it's written this way:** nulling `password` avoids leaking the hash (or any password field) to the client.
- **Step by step:**
  1. Validate `RegisterRequest`.
  2. Call `authService.register`.
  3. Set `password` to `null` on the saved user.
  4. Return `201` with the user, or an error map on failure.

### `public ResponseEntity<?> sendRegistrationOtp(@Valid @RequestBody SendOtpRequest request)`
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
- **What it does:** starts OTP signup by saving pending registration data and emailing a code.
- **Where it's called:** by Spring MVC on `POST /api/auth/register/send-otp`.
- **Why it's written this way:** the controller only confirms that a code was sent; it does not return the OTP.
- **Step by step:**
  1. Validate `SendOtpRequest`.
  2. Call `authService.startRegistration`.
  3. Return `200` with a success message, or an error map.

### `public ResponseEntity<?> verifyRegistrationOtp(@Valid @RequestBody VerifyOtpRequest request)`
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
- **What it does:** checks the signup OTP, creates the real user, and returns a login-style response (token + user).
- **Where it's called:** by Spring MVC on `POST /api/auth/register/verify-otp`.
- **Why it's written this way:** one endpoint finishes signup and logs the user in by returning the same shape as login.
- **Step by step:**
  1. Validate `VerifyOtpRequest`.
  2. Call `authService.verifyRegistration`.
  3. Return `200` with that result, or an error map.

### `public ResponseEntity<?> resendRegistrationOtp(@Valid @RequestBody ResendOtpRequest request)`
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
- **What it does:** emails a new signup OTP for an existing pending registration.
- **Where it's called:** by Spring MVC on `POST /api/auth/register/resend-otp`.
- **Why it's written this way:** only the email is needed; the service owns rate limits and expiry rules.
- **Step by step:**
  1. Validate `ResendOtpRequest`.
  2. Call `authService.resendOtp` with the email.
  3. Return a success message or error map.

### `public ResponseEntity<?> sendPasswordResetOtp(@Valid @RequestBody ForgotPasswordRequest request)`
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
- **What it does:** starts forgot-password by emailing a reset OTP (service returns 404 if the email is unknown).
- **Where it's called:** by Spring MVC on `POST /api/auth/forgot-password/send-otp`.
- **Why it's written this way:** keeps the HTTP message user-friendly while the service decides whether the email exists.
- **Step by step:**
  1. Validate `ForgotPasswordRequest`.
  2. Call `authService.startPasswordReset`.
  3. Return success or error JSON.

### `public ResponseEntity<?> verifyPasswordResetOtp(@Valid @RequestBody VerifyOtpRequest request)`
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
- **What it does:** checks the reset OTP and returns a short-lived `resetToken` the app must send when setting the new password.
- **Where it's called:** by Spring MVC on `POST /api/auth/forgot-password/verify-otp`.
- **Why it's written this way:** wrapping the string in `ResetTokenResponse` gives a clean JSON field instead of a bare string.
- **Step by step:**
  1. Validate `VerifyOtpRequest`.
  2. Call `authService.verifyPasswordResetOtp`.
  3. Return `200` with `ResetTokenResponse`, or an error map.

### `public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request)`
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
- **What it does:** sets a new password after the reset token is checked by the service.
- **Where it's called:** by Spring MVC on `POST /api/auth/forgot-password/reset`.
- **Why it's written this way:** the controller stays thin; all token and hashing rules live in `AuthService`.
- **Step by step:**
  1. Validate `ResetPasswordRequest`.
  2. Call `authService.resetPassword`.
  3. Return a success message or error map.
