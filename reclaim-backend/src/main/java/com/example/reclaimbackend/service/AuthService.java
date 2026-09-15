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
 * Login, signup, and password reset.
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

    // ---
    // Login
    // ---

    /**
     * Checks email and password, then returns a token.
     *
     * @param request email and password
     * @return a token for the user
     * @throws ResponseStatusException 401 if email or password is wrong
     */
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

    // ---
    // Sign up
    // ---

    /**
     * Creates a user if the email is free.
     *
     * @param request email, password, name, phone
     * @return the saved user
     * @throws ResponseStatusException 409 if the email is taken
     */
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

    // ---
    // Sign-up code
    // ---

    /**
     * Saves the signup details and emails a 6-digit code.
     * Replaces any older pending record. The user is not created yet.
     *
     * @param request the signup details
     * @throws ResponseStatusException 409 if the email already has an account
     */
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

    /**
     * Sends a new code for a signup that is still pending.
     *
     * @param email the email waiting to be checked
     * @throws ResponseStatusException 400 if there is no pending signup
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
     * Checks the code, then creates the user and returns a token.
     *
     * @param request the email and code
     * @return a token so the user is signed in
     * @throws ResponseStatusException 400/410/429 if the code is bad, expired, or tried too often
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

    // ---
    // Forgot password
    // ---

    /**
     * Emails a reset code. Stops with 404 if the email is unknown.
     */
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

    /**
     * Checks the reset code and returns a token used to set a new password.
     *
     * @param request the email and code
     * @return the reset token
     * @throws ResponseStatusException 400/410/429 if the code is bad, expired, or tried too often
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
     * Checks the reset token, then saves the new password.
     *
     * @param request email, reset token, and new password
     * @throws ResponseStatusException 400/410 if the token is bad or expired
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

    /** Makes a 6-digit code. */
    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
