package com.example.reclaimbackend.service;

import com.example.reclaimbackend.config.JwtUtil;
import com.example.reclaimbackend.dto.LoginRequest;
import com.example.reclaimbackend.dto.LoginResponse;
import com.example.reclaimbackend.dto.RegisterRequest;
import com.example.reclaimbackend.model.User;
import com.example.reclaimbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

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
                    logger.warn("Login failed: email not found — {}", request.getEmail());
                    return new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid email or password");
                });

        // 2. Verify the raw password against the BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Login failed: incorrect password for — {}", request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password");
        }

        // 3. Generate a signed JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        logger.info("Login successful for user: {} (ID: {})",
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
            logger.warn("Registration failed: email already exists — {}",
                    request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        // 2. Create the user entity and hash the password
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhoneNumber(request.getPhoneNumber());

        // 3. Save to MongoDB
        User savedUser = userRepository.save(user);

        logger.info("User registered successfully: {} (ID: {})",
                savedUser.getEmail(), savedUser.getId());

        return savedUser;
    }
}
