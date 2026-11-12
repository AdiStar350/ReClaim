package com.example.reclaimbackend.service;

import com.example.reclaimbackend.config.JwtUtil;
import com.example.reclaimbackend.dto.LoginRequest;
import com.example.reclaimbackend.dto.LoginResponse;
import com.example.reclaimbackend.dto.SendOtpRequest;
import com.example.reclaimbackend.dto.VerifyOtpRequest;
import com.example.reclaimbackend.model.PendingRegistration;
import com.example.reclaimbackend.model.User;
import com.example.reclaimbackend.repository.PasswordResetTokenRepository;
import com.example.reclaimbackend.repository.PendingRegistrationRepository;
import com.example.reclaimbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_rejectsWrongPassword() {
        User user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .password("hashed")
                .name("Test User")
                .phoneNumber("0500000000")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("user@example.com", "wrong-password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void startPasswordReset_rejectsUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.startPasswordReset("missing@example.com"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void startRegistration_and_verifyRegistration_completesFullFlow() {
        SendOtpRequest sendRequest = SendOtpRequest.builder()
                .email("newuser@example.com")
                .password("secret123")
                .name("New User")
                .phoneNumber("0501234567")
                .profilePictureUrl("https://example.com/avatar.jpg")
                .build();

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenAnswer(inv -> {
            String raw = inv.getArgument(0).toString();
            return "secret123".equals(raw) ? "hashed-password" : "hashed-otp";
        });

        authService.startRegistration(sendRequest);

        ArgumentCaptor<PendingRegistration> pendingCaptor =
                ArgumentCaptor.forClass(PendingRegistration.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);

        verify(pendingRegistrationRepository).deleteByEmail("newuser@example.com");
        verify(pendingRegistrationRepository).save(pendingCaptor.capture());
        verify(emailService).sendOtpEmail(eq("newuser@example.com"), otpCaptor.capture());

        PendingRegistration savedPending = pendingCaptor.getValue();
        String issuedCode = otpCaptor.getValue();

        assertTrue(issuedCode.matches("\\d{6}"));
        assertEquals("newuser@example.com", savedPending.getEmail());
        assertEquals("hashed-password", savedPending.getPasswordHash());
        assertEquals("hashed-otp", savedPending.getOtpHash());
        assertEquals(0, savedPending.getAttempts());
        assertNotNull(savedPending.getExpiresAt());
        assertTrue(savedPending.getExpiresAt().isAfter(Instant.now()));

        when(pendingRegistrationRepository.findByEmail("newuser@example.com"))
                .thenReturn(Optional.of(savedPending));
        when(passwordEncoder.matches(issuedCode, "hashed-otp")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-new-1");
            return u;
        });
        when(jwtUtil.generateToken("user-new-1", "newuser@example.com")).thenReturn("jwt-token-123");

        LoginResponse response = authService.verifyRegistration(
                new VerifyOtpRequest("newuser@example.com", issuedCode));

        assertEquals("jwt-token-123", response.getToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(pendingRegistrationRepository).delete(savedPending);

        User createdUser = userCaptor.getValue();
        assertEquals("newuser@example.com", createdUser.getEmail());
        assertEquals("hashed-password", createdUser.getPassword());
        assertEquals("New User", createdUser.getName());
        assertEquals("0501234567", createdUser.getPhoneNumber());
        assertEquals("https://example.com/avatar.jpg", createdUser.getProfilePictureUrl());
    }

    @Test
    void startRegistration_rejectsAlreadyRegisteredEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        SendOtpRequest request = SendOtpRequest.builder()
                .email("taken@example.com")
                .password("secret123")
                .name("Taken")
                .build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.startRegistration(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void verifyRegistration_rejectsInvalidOtpAndIncrementsAttempts() {
        PendingRegistration pending = PendingRegistration.builder()
                .email("newuser@example.com")
                .otpHash("hashed-otp")
                .attempts(0)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(pendingRegistrationRepository.findByEmail("newuser@example.com"))
                .thenReturn(Optional.of(pending));
        when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verifyRegistration(
                        new VerifyOtpRequest("newuser@example.com", "000000")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(1, pending.getAttempts());
        verify(pendingRegistrationRepository).save(pending);
    }
}
