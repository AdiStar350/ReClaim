package com.example.reclaimbackend.controller;

import com.example.reclaimbackend.dto.ForgotPasswordRequest;
import com.example.reclaimbackend.dto.LoginRequest;
import com.example.reclaimbackend.dto.LoginResponse;
import com.example.reclaimbackend.dto.RegisterRequest;
import com.example.reclaimbackend.dto.ResendOtpRequest;
import com.example.reclaimbackend.dto.ResetPasswordRequest;
import com.example.reclaimbackend.dto.ResetTokenResponse;
import com.example.reclaimbackend.dto.SendOtpRequest;
import com.example.reclaimbackend.dto.VerifyOtpRequest;
import com.example.reclaimbackend.model.User;
import com.example.reclaimbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * REST controller for authentication endpoints.
 * <p>
 * All endpoints under {@code /api/auth} are publicly accessible
 * (configured in {@link com.example.reclaimbackend.config.SecurityConfig}).
 * </p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code POST /api/auth/login} — Authenticate and receive a JWT.</li>
 *   <li>{@code POST /api/auth/register} — Create a new account.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns a JWT token.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/auth/login}<br>
     * <b>Access:</b> Public<br>
     * <b>Request body:</b> {@link LoginRequest} (email + password)<br>
     * <b>Success response:</b> 200 OK → {@link LoginResponse} (token)<br>
     * <b>Error response:</b> 401 Unauthorized → error message
     * </p>
     *
     * @param request the login credentials
     * @return 200 OK with JWT token, or 401 Unauthorized
     */
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

    /**
     * Registers a new user account.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/auth/register}<br>
     * <b>Access:</b> Public<br>
     * <b>Request body:</b> {@link RegisterRequest}
     *   (email, password, name, phoneNumber)<br>
     * <b>Success response:</b> 201 Created → saved user
     *   (password field excluded)<br>
     * <b>Error response:</b> 409 Conflict → "Email is already registered"
     * </p>
     *
     * @param request the registration data
     * @return 201 Created with user data, or 409 Conflict
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User savedUser = authService.register(request);
            // Never return the hashed password to the client
            savedUser.setPassword(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (ResponseStatusException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        }
    }

    /**
     * Step 1 of OTP registration: stores the registration payload and emails a
     * 6-digit verification code.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/auth/register/send-otp}<br>
     * <b>Success:</b> 200 OK → {@code {"message": "..."}}<br>
     * <b>Error:</b> 409 if the email already has an account.
     * </p>
     */
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

    /**
     * Step 2 of OTP registration: validates the code, creates the user in
     * MongoDB, and returns a JWT so the client is logged in immediately.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/auth/register/verify-otp}<br>
     * <b>Success:</b> 200 OK → {@link LoginResponse}<br>
     * <b>Error:</b> 400 invalid, 410 expired, 429 too many attempts.
     * </p>
     */
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

    /**
     * Re-sends a fresh OTP for a pending registration.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/auth/register/resend-otp}
     * </p>
     */
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

    // ═════════════════════════════════════════════════════════════════════
    //  FORGOT PASSWORD
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Step 1: emails a 6-digit reset code.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/auth/forgot-password/send-otp}<br>
     * Always returns 200 (does not reveal whether the email exists).
     * </p>
     */
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendPasswordResetOtp(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.startPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "If an account exists for this email, a reset code has been sent"));
    }

    /**
     * Step 2: validates the reset code and returns an opaque reset token.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/auth/forgot-password/verify-otp}<br>
     * <b>Success:</b> 200 OK → {@link ResetTokenResponse}<br>
     * <b>Error:</b> 400 invalid, 410 expired, 429 too many attempts.
     * </p>
     */
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

    /**
     * Step 3: sets the new password after validating the reset token.
     * <p>
     * <b>Endpoint:</b> {@code POST /api/auth/forgot-password/reset}
     * </p>
     */
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
}
