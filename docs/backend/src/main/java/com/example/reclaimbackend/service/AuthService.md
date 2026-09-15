```java
public LoginResponse login(LoginRequest request) {
    // Look up the user
    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.warn("Login failed: email not found — {}", request.getEmail());
                return new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password");
            });

    // Check the password
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        log.warn("Login failed: incorrect password for — {}", request.getEmail());
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password");
    }

    // Make a token
    String token = jwtUtil.generateToken(user.getId(), user.getEmail());

    log.info("Login successful for user: {} (ID: {})",
            user.getEmail(), user.getId());

    return new LoginResponse(token);
}
```

What that function does: I check email and password, then return a token.
What the function is responsible for: I sign the user in. Wrong email or password both look the same to the caller.
Where it works: I am called by AuthController.login from POST /api/auth/login. AuthServiceTest.login_rejectsWrongPassword checks me.
Why I chose to write the code of the function the way its written: I use the same 401 text for missing email and bad password so a caller cannot tell which failed.
More: I use UserRepository.findByEmail, PasswordEncoder.matches, JwtUtil.generateToken, and slf4j log.

```java
public User register(RegisterRequest request) {
    // Stop if the email is taken
    if (userRepository.existsByEmail(request.getEmail())) {
        log.warn("Registration failed: email already exists — {}",
                request.getEmail());
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email is already registered");
    }

    // Hash the password and build the user
    User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .phoneNumber(request.getPhoneNumber())
            .build();

    // Save the user
    User savedUser = userRepository.save(user);

    log.info("User registered successfully: {} (ID: {})",
            savedUser.getEmail(), savedUser.getId());

    return savedUser;
}
```

What that function does: I create a user if the email is free, with a hashed password.
What the function is responsible for: I am the direct signup path without an email code.
Where it works: I am called by AuthController.register from POST /api/auth/register.
Why I chose to write the code of the function the way its written: I hash before save so the raw password never sits in Mongo. 409 stops a duplicate email.
More: I use existsByEmail, PasswordEncoder.encode, User.builder, and userRepository.save.

```java
public void startRegistration(SendOtpRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        log.warn("OTP request failed: email already exists — {}", request.getEmail());
        throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email is already registered");
    }

    // Drop any leftover pending record for this email.
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

What that function does: I save signup details, email a 6-digit code, and do not create the user yet.
What the function is responsible for: I start the coded signup. I replace any older pending row for that email.
Where it works: I am called by AuthController.sendRegistrationOtp from POST /api/auth/register/send-otp.
Why I chose to write the code of the function the way its written: I hash password and code, then send the raw code only by email. deleteByEmail avoids two pending rows.
More: I use Instant, ChronoUnit.MINUTES, otpExpiryMinutes, EmailService.sendOtpEmail, and generateOtp.

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

What that function does: I send a new signup code for a pending email and reset tries.
What the function is responsible for: I refresh the code without asking for signup details again.
Where it works: I am called by AuthController.resendRegistrationOtp from POST /api/auth/register/resend-otp.
Why I chose to write the code of the function the way its written: I reset attempts and expiry so a new code is a clean try. 400 if nothing is pending.
More: I use findByEmail, PasswordEncoder.encode, and EmailService.sendOtpEmail.

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

    // Stop if someone else registered this email first.
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

What that function does: I check the code, create the user from the pending row, and return a token.
What the function is responsible for: I finish coded signup. I drop the pending row after success or when the code is dead.
Where it works: I am called by AuthController.verifyRegistrationOtp from POST /api/auth/register/verify-otp.
Why I chose to write the code of the function the way its written: I count wrong tries, expire the code, and copy the already-hashed password onto the user so I do not hash twice.
More: I use HttpStatus.GONE and TOO_MANY_REQUESTS, PasswordEncoder.matches, and JwtUtil.generateToken.

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

What that function does: I email a reset code, or stop with 404 if the email is unknown.
What the function is responsible for: I start forgot-password. I replace any older reset row.
Where it works: I am called by AuthController.sendPasswordResetOtp from POST /api/auth/forgot-password/send-otp. AuthServiceTest.startPasswordReset_rejectsUnknownEmail checks me.
Why I chose to write the code of the function the way its written: I return 404 for unknown email as the controller comment says. I hash the code before save.
More: I use Optional, deleteByEmail, EmailService.sendPasswordResetEmail, and PasswordResetToken.builder.

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

What that function does: I check the reset code and return a random token used to set a new password.
What the function is responsible for: I mark the reset as verified. The code itself is no longer enough to change the password.
Where it works: I am called by AuthController.verifyPasswordResetOtp from POST /api/auth/forgot-password/verify-otp.
Why I chose to write the code of the function the way its written: I glue two UUIDs so the reset token is long and not guessable. I extend expiry for the next step.
More: I use UUID.randomUUID, PasswordEncoder.matches, and the same attempt/expiry checks as signup.

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

What that function does: I check the reset token, save the new hashed password, and drop the reset row.
What the function is responsible for: I finish forgot-password.
Where it works: I am called by AuthController.resetPassword from POST /api/auth/forgot-password/reset.
Why I chose to write the code of the function the way its written: I require verified plus matching resetToken so a stolen email alone cannot change the password. I delete the row so the token cannot be reused.
More: I use PasswordEncoder.encode, findByEmail, and passwordResetTokenRepository.delete.

```java
private String generateOtp() {
    return String.format("%06d", secureRandom.nextInt(1_000_000));
}
```

What that function does: I make a 6-digit code, including leading zeros.
What the function is responsible for: I am the shared code generator for signup and password reset.
Where it works: I am called by startRegistration, resendOtp, and startPasswordReset. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because %06d keeps codes like 004231 as six characters. SecureRandom is harder to guess than Random.
More: I use java.security.SecureRandom nextInt(1_000_000) and String.format.
