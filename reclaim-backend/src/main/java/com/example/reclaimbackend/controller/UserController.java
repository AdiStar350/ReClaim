package com.example.reclaimbackend.controller;

import com.example.reclaimbackend.dto.DeleteAccountRequest;
import com.example.reclaimbackend.dto.UpdateProfileRequest;
import com.example.reclaimbackend.dto.UserResponse;
import com.example.reclaimbackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authenticated user profile endpoints.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        userService.deleteAccount(userId, request.getName(), request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
