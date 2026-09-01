package com.example.reclaimbackend.service;

import com.example.reclaimbackend.config.JwtUtil;
import com.example.reclaimbackend.dto.LoginRequest;
import com.example.reclaimbackend.dto.LoginResponse;
import com.example.reclaimbackend.dto.RegisterRequest;
import com.example.reclaimbackend.dto.ResetPasswordRequest;
import com.example.reclaimbackend.dto.SendOtpRequest;
import com.example.reclaimbackend.dto.VerifyOtpRequest;
import com.example.reclaimbackend.model.PasswordResetToken;
import com.example.reclaimbackend.model.PendingRegistration;
import com.example.reclaimbackend.model.User;
import com.example.reclaimbackend.repository.PasswordResetTokenRepository;
import com.example.reclaimbackend.repository.PendingRegistrationRepository;
import com.example.reclaimbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for authentication and registration operations.
 * <p>
 * Handles:
 * <ul>
 *   <li><b>Login:</b> Verifies email/password credentials against the
 *       BCrypt hash stored in MongoDB. On success, issues a signed JWT
 *       via {@link JwtUtil}.</li>
 *   <li><b>Register:</b> Checks for duplicate emails, hashes the
 *       password with BCrypt, and persists the new user to MongoDB.</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.expiry.minutes:10}")
    private long otpExpiryMinutes;

    @Value("${otp.max.attempts:5}")
    private int otpMaxAttempts;

    // ═════════════════════════════════════════════════════════════════════
    //  LOGIN
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Authenticates a user with the provided credentials.
     * <p>
     * <b>Flow:</b>
     * <ol>
     *   <li>Look up the user by email in MongoDB.</li>
     *   <li>Verify the raw password against the stored BCrypt hash
     *       using {@link PasswordEncoder#matches}.</li>
     *   <li>Generate a signed JWT token containing the user's ID
     *       (as subject) and email (as a custom claim).</li>
     *   <li>Return the token wrapped in a {@link LoginResponse}.</li>
     * </ol>
     * </p>
     *
     * @param request the login credentials (email + password)
     * @return a {@link LoginResponse} containing the JWT token
     * @throws ResponseStatusException 401 if the email is not found
     *         or the password does not match
     */
    public LoginResponse login(LoginRequest request) {
        // 1. Find the user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: email not found — {}", request.getEmail());
                    return new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid email or password");
                });

        // 2. Verify the raw password against the BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: incorrect password for — {}", request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password");
        }

        // 3. Generate a signed JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        log.info("Login successful for user: {} (ID: {})",
                user.getEmail(), user.getId());

        return new LoginResponse(token);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  REGISTER
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Registers a new user account.
     * <p>
     * <b>Flow:</b>
     * <ol>
     *   <li>Check if a user with the provided email already exists.
     *       If so, throw a 409 Conflict.</li>
     *   <li>Hash the plain-text password with BCrypt.</li>
     *   <li>Create and save the {@link User} document to MongoDB.</li>
     *   <li>Return the saved user (the caller should null-out the
     *       password before returning it to the client).</li>
     * </ol>
     * </p>
     *
     * @param request the registration data (email, password, name, phoneNumber)
     * @return the saved {@link User} document with generated MongoDB ID
     * @throws ResponseStatusException 409 if the email is already registered
     */
    public User register(RegisterRequest request) {
        // 1. Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists — {}",
                    request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        // 2. Create the user entity and hash the password
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .build();

        // 3. Save to MongoDB
        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {} (ID: {})",
                savedUser.getEmail(), savedUser.getId());

        return savedUser;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  REGISTRATION WITH EMAIL OTP
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Starts a registration by storing the payload in a pending record and
     * emailing a fresh 6-digit OTP. Any previous pending record for the same
     * email is replaced. The user document is not created yet.
     *
     * @param request the full registration payload
     * @throws ResponseStatusException 409 if the email already has an account
     */
    public void startRegistration(SendOtpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("OTP request failed: email already exists — {}", request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email is already registered");
        }

        // Replace any stale pending record for this email.
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

    /**
     * Regenerates and re-sends the OTP for an existing pending registration.
     *
     * @param email the email awaiting verification
     * @throws ResponseStatusException 400 if there is no pending registration
     */
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

    /**
     * Validates the submitted OTP and, on success, promotes the pending record
     * into a real {@link User} document in MongoDB and issues a JWT so the
     * client is logged in immediately.
     *
     * @param request the email + submitted code
     * @return a {@link LoginResponse} with a signed JWT
     * @throws ResponseStatusException 400/410/429 for invalid/expired/too-many
     */
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

        // Guard against a race where the email was registered meanwhile.
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

    // ═════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD (email OTP → reset token → new password)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Starts a password reset by emailing a fresh 6-digit OTP.
     * <p>
     * To avoid leaking which emails are registered, this always succeeds from
     * the caller's perspective; a code is only generated and emailed when an
     * account actually exists for the address.
     * </p>
     *
     * @param email the account email requesting a reset
     */
    public void startPasswordReset(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            log.info("Password reset requested for unknown email — {} (ignored)", email);
            return;
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

    /**
     * Validates a password-reset OTP and, on success, issues an opaque reset
     * token the client must present to actually change the password.
     *
     * @param request the email + submitted code
     * @return the reset token
     * @throws ResponseStatusException 400/410/429 for invalid/expired/too-many
     */
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

    /**
     * Completes a password reset: verifies the reset token, then hashes and
     * stores the new password in MongoDB.
     *
     * @param request email + reset token + new password
     * @throws ResponseStatusException 400/410 for invalid/expired reset token
     */
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

    /** Generates a zero-padded 6-digit one-time code. */
    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
