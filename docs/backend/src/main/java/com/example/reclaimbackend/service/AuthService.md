# AuthService.java

## Overview
This is a **service** for login, signup (including **OTP**), and forgot-password flows (see `docs/GLOSSARY.md`). It hashes passwords and OTP codes, emails codes via `EmailService`, stores pending signup / reset records in MongoDB, and issues **JWTs** through `JwtUtil` after a successful login or verified registration.

## Concepts used here
- **Service** — auth logic behind `AuthController`; see `docs/GLOSSARY.md`.
- **JWT** — returned after login or OTP signup verify; see `docs/GLOSSARY.md`.
- **Password hashing** — `PasswordEncoder` stores and checks passwords/OTP hashes; see `docs/GLOSSARY.md`.
- **OTP** — 6-digit emailed codes with expiry and attempt limits; see `docs/GLOSSARY.md`.
- **Repository / model** — `User`, `PendingRegistration`, `PasswordResetToken`; see `docs/GLOSSARY.md`.
- **DTO** — login/register/OTP request and response types; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring** (`@Service`, `@Value`, `HttpStatus`, `ResponseStatusException`, `PasswordEncoder`): service bean, OTP config, API errors, hashing.
- **Lombok** (`@RequiredArgsConstructor`, `@Slf4j`): DI and logging.
- **Java security / time** (`SecureRandom`, `Instant`, `UUID`): OTP generation, expiry, reset tokens.
- **Project**: repositories, DTOs, models, `JwtUtil`, `EmailService`.

## Fields
- `userRepository` / `pendingRegistrationRepository` / `passwordResetTokenRepository` — persistence.
- `passwordEncoder` (`PasswordEncoder`) — hashes passwords and OTP codes; verifies matches.
- `jwtUtil` (`JwtUtil`) — creates JWT strings.
- `emailService` (`EmailService`) — sends OTP emails.
- `secureRandom` (`SecureRandom`) — generates OTPs.
- `otpExpiryMinutes` (`long`) — how long a code lasts (default 10).
- `otpMaxAttempts` (`int`) — wrong-code limit (default 5).

## Functions

### `private String generateOtp()`
```java
private String generateOtp() {
    return String.format("%06d", secureRandom.nextInt(1_000_000));
}
```
- **What it does:** returns a zero-padded 6-digit code (`000000`–`999999`).
- **Where it's called:** `startRegistration`, `resendOtp`, and `startPasswordReset` in this class.
- **Why it's written this way:** `SecureRandom` is harder to predict than `Random`; formatting keeps leading zeros.
- **Step by step:**
  1. Draw an int in `0..999999`.
  2. Format as six digits and return.

### `public LoginResponse login(LoginRequest request)`
```java
public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.warn("Login failed: email not found — {}", request.getEmail());
                return new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password");
            });

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        log.warn("Login failed: incorrect password for — {}", request.getEmail());
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password");
    }

    String token = jwtUtil.generateToken(user.getId(), user.getEmail());

    log.info("Login successful for user: {} (ID: {})",
            user.getEmail(), user.getId());

    return new LoginResponse(token);
}
```
- **What it does:** verifies email/password and returns a JWT; wrong email or password both look like `401 Invalid email or password`.
- **Where it's called:** `AuthController.login`.
- **Why it's written this way:** the same error text for missing email vs bad password avoids revealing which accounts exist.
- **Step by step:**
  1. Find user by email or throw 401.
  2. Check password hash or throw 401.
  3. Generate JWT and return `LoginResponse`.

### `public User register(RegisterRequest request)`
```java
public User register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        log.warn("Registration failed: email already exists — {}",
                request.getEmail());
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email is already registered");
    }

    User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .phoneNumber(request.getPhoneNumber())
            .build();

    User savedUser = userRepository.save(user);

    log.info("User registered successfully: {} (ID: {})",
            savedUser.getEmail(), savedUser.getId());

    return savedUser;
}
```
- **What it does:** creates a user immediately if the email is free (direct register path without OTP).
- **Where it's called:** `AuthController.register`.
- **Why it's written this way:** hashes the password before save; returns the entity so the controller can strip the password field.
- **Step by step:**
  1. Reject if email exists (`409`).
  2. Build user with hashed password.
  3. Save and return.

### `public void startRegistration(SendOtpRequest request)`
```java
public void startRegistration(SendOtpRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        log.warn("OTP request failed: email already exists — {}", request.getEmail());
        throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email is already registered");
    }

    pendingRegistrationRepository.deleteByEmail(request.getEmail());

    String code = generateOtp();
    Instant now = Instant.now();
    PendingRegistration pending = PendingRegistration.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .phoneNumber(request.getPhoneNumber())
            .profilePictureUrl(request.getProfilePictureUrl())
            .otpHash(passwordEncoder.encode(code))
            .attempts(0)
            .createdAt(now)
            .expiresAt(now.plus(otpExpiryMinutes, ChronoUnit.MINUTES))
            .build();

    pendingRegistrationRepository.save(pending);
    emailService.sendOtpEmail(request.getEmail(), code);
    log.info("Registration OTP issued for {}", request.getEmail());
}
```
- **What it does:** stores signup details + hashed OTP as a pending registration and emails the plain code.
- **Where it's called:** `AuthController.sendRegistrationOtp`.
- **Why it's written this way:** the real `User` is not created until verify; deleting any old pending row avoids duplicates; OTP is hashed like a password.
- **Step by step:**
  1. Reject if email already has an account.
  2. Delete any prior pending record for that email.
  3. Generate OTP, build/save `PendingRegistration` with hashed password and OTP.
  4. Email the plain code.

### `public void resendOtp(String email)`
```java
public void resendOtp(String email) {
    PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No pending registration for this email"));

    String code = generateOtp();
    Instant now = Instant.now();
    pending.setOtpHash(passwordEncoder.encode(code));
    pending.setAttempts(0);
    pending.setExpiresAt(now.plus(otpExpiryMinutes, ChronoUnit.MINUTES));
    pendingRegistrationRepository.save(pending);

    emailService.sendOtpEmail(email, code);
    log.info("Registration OTP resent for {}", email);
}
```
- **What it does:** issues a fresh signup OTP for an existing pending registration.
- **Where it's called:** `AuthController.resendRegistrationOtp`.
- **Why it's written this way:** resets attempts and expiry so a new code is usable.
- **Step by step:**
  1. Load pending or throw 400.
  2. Generate new code, update hash/attempts/expiry, save.
  3. Email the code.

### `public LoginResponse verifyRegistration(VerifyOtpRequest request)`
```java
public LoginResponse verifyRegistration(VerifyOtpRequest request) {
    PendingRegistration pending = pendingRegistrationRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No pending registration for this email"));

    if (pending.getExpiresAt() == null || pending.getExpiresAt().isBefore(Instant.now())) {
        pendingRegistrationRepository.delete(pending);
        throw new ResponseStatusException(
                HttpStatus.GONE, "Verification code expired. Please request a new one.");
    }

    if (pending.getAttempts() >= otpMaxAttempts) {
        pendingRegistrationRepository.delete(pending);
        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many incorrect attempts. Please request a new code.");
    }

    if (!passwordEncoder.matches(request.getCode(), pending.getOtpHash())) {
        pending.setAttempts(pending.getAttempts() + 1);
        pendingRegistrationRepository.save(pending);
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid verification code");
    }

    if (userRepository.existsByEmail(pending.getEmail())) {
        pendingRegistrationRepository.delete(pending);
        throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email is already registered");
    }

    User user = User.builder()
            .email(pending.getEmail())
            .password(pending.getPasswordHash())
            .name(pending.getName())
            .phoneNumber(pending.getPhoneNumber())
            .profilePictureUrl(pending.getProfilePictureUrl())
            .build();

    User savedUser = userRepository.save(user);
    pendingRegistrationRepository.delete(pending);

    String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail());
    log.info("Registration finalized via OTP: {} (ID: {})",
            savedUser.getEmail(), savedUser.getId());

    return new LoginResponse(token);
}
```
- **What it does:** validates the signup OTP (expiry/attempts), creates the user from pending data, deletes the pending row, and returns a JWT.
- **Where it's called:** `AuthController.verifyRegistrationOtp`.
- **Why it's written this way:** checks race (email taken after OTP was sent); reuses the already-hashed password from pending.
- **Step by step:**
  1. Load pending or 400.
  2. Reject expired (`410`) or too many attempts (`429`), deleting the pending row.
  3. On wrong code, increment attempts and return 400.
  4. Create user, delete pending, return JWT.

### `public void startPasswordReset(String email)`
```java
public void startPasswordReset(String email) {
    Optional<User> user = userRepository.findByEmail(email);
    if (user.isEmpty()) {
        log.info("Password reset requested for unknown email — {}", email);
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No account found with this email");
    }

    passwordResetTokenRepository.deleteByEmail(email);

    String code = generateOtp();
    Instant now = Instant.now();
    PasswordResetToken token = PasswordResetToken.builder()
            .email(email)
            .otpHash(passwordEncoder.encode(code))
            .attempts(0)
            .verified(false)
            .resetToken(null)
            .createdAt(now)
            .expiresAt(now.plus(otpExpiryMinutes, ChronoUnit.MINUTES))
            .build();

    passwordResetTokenRepository.save(token);
    emailService.sendPasswordResetEmail(email, code);
    log.info("Password reset OTP issued for {}", email);
}
```
- **What it does:** starts forgot-password for a known email by saving a reset OTP record and emailing the code.
- **Where it's called:** `AuthController.sendPasswordResetOtp`.
- **Why it's written this way:** unknown emails get `404` (explicit for this app); old reset rows are cleared first.
- **Step by step:**
  1. Require an existing user or throw 404.
  2. Delete prior reset token for the email.
  3. Save new hashed OTP record and email the code.

### `public String verifyPasswordResetOtp(VerifyOtpRequest request)`
```java
public String verifyPasswordResetOtp(VerifyOtpRequest request) {
    PasswordResetToken token = passwordResetTokenRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No password reset request for this email"));

    if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
        passwordResetTokenRepository.delete(token);
        throw new ResponseStatusException(
                HttpStatus.GONE, "Reset code expired. Please request a new one.");
    }

    if (token.getAttempts() >= otpMaxAttempts) {
        passwordResetTokenRepository.delete(token);
        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many incorrect attempts. Please request a new code.");
    }

    if (!passwordEncoder.matches(request.getCode(), token.getOtpHash())) {
        token.setAttempts(token.getAttempts() + 1);
        passwordResetTokenRepository.save(token);
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid reset code");
    }

    String resetToken = UUID.randomUUID().toString() + UUID.randomUUID();
    Instant now = Instant.now();
    token.setVerified(true);
    token.setResetToken(resetToken);
    token.setAttempts(0);
    token.setExpiresAt(now.plus(otpExpiryMinutes, ChronoUnit.MINUTES));
    passwordResetTokenRepository.save(token);

    log.info("Password reset OTP verified for {}", request.getEmail());
    return resetToken;
}
```
- **What it does:** checks the reset OTP, then marks the record verified and returns a random `resetToken` for the final password change.
- **Where it's called:** `AuthController.verifyPasswordResetOtp`.
- **Why it's written this way:** a second token (not the OTP) is required to set the password, so intercepting only the email is not enough.
- **Step by step:**
  1. Load reset record; check expiry/attempts like signup.
  2. On success, set `verified`, store a long random `resetToken`, refresh expiry.
  3. Return that reset token string.

### `public void resetPassword(ResetPasswordRequest request)`
```java
public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken token = passwordResetTokenRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired reset request"));

    if (!token.isVerified() || token.getResetToken() == null
            || !token.getResetToken().equals(request.getResetToken())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid or expired reset request");
    }

    if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
        passwordResetTokenRepository.delete(token);
        throw new ResponseStatusException(
                HttpStatus.GONE, "Reset session expired. Please start over.");
    }

    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Account no longer exists"));

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
    passwordResetTokenRepository.delete(token);

    log.info("Password reset completed for {} (ID: {})",
            user.getEmail(), user.getId());
}
```
- **What it does:** verifies the reset token, hashes and saves the new password, then deletes the reset record.
- **Where it's called:** `AuthController.resetPassword`.
- **Why it's written this way:** one-time use of the reset session; password never stored in plain text.
- **Step by step:**
  1. Load token; require verified + matching `resetToken`.
  2. Reject if expired (`410`).
  3. Load user, encode new password, save, delete token.
