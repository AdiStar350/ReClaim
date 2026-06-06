package com.example.reclaimbackend.controller;

import com.example.reclaimbackend.dto.LoginRequest;
import com.example.reclaimbackend.dto.LoginResponse;
import com.example.reclaimbackend.dto.RegisterRequest;
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
}
