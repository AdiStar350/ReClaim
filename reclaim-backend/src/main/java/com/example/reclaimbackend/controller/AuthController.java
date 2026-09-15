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
 * Login, signup, and password-reset endpoints.
 * These {@code /api/auth} routes are public.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Checks email and password, then returns a token.
     *
     * @param request email and password
     * @return 200 with a token, or 401
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
     * Creates a new account. The password is stripped before the reply.
     *
     * @param request the signup details
     * @return 201 with the user, or 409 if the email is taken
     */
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

    /**
     * Saves the signup details and emails a 6-digit code.
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
     * Checks the signup code, creates the user, and returns a token.
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
     * Sends a new signup code.
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

    // ---
    // Forgot password
    // ---

    /**
     * Emails a 6-digit reset code. Returns 404 if the email is not registered.
     */
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

    /**
     * Checks the reset code and returns a token used to set a new password.
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
     * Sets the new password after checking the reset token.
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
