package com.example.reclaimbackend.service;

import com.example.reclaimbackend.dto.UpdateProfileRequest;
import com.example.reclaimbackend.dto.UserResponse;
import com.example.reclaimbackend.model.User;
import com.example.reclaimbackend.repository.ClaimRepository;
import com.example.reclaimbackend.repository.ItemRepository;
import com.example.reclaimbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Profile reads, updates, and account deletion.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final PasswordEncoder passwordEncoder;
    private final FirebaseStorageService firebaseStorageService;

    public UserResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Email is already registered");
            }
            user.setEmail(request.getEmail().trim());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getProfilePictureUrl() != null) {
            String url = request.getProfilePictureUrl().trim();
            String newUrl = url.isEmpty() ? null : url;
            String previousUrl = user.getProfilePictureUrl();
            if (!Objects.equals(blankToNull(previousUrl), newUrl)) {
                firebaseStorageService.deleteReplacedImage(previousUrl, newUrl);
            }
            user.setProfilePictureUrl(newUrl);
        }

        return toResponse(userRepository.save(user));
    }

    public User getUserEntity(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * Saves the device token used to send this user notifications.
     */
    public void updateFcmToken(String userId, String fcmToken) {
        User user = getUserEntity(userId);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    /**
     * Deletes the user after checking name and password.
     * Also removes their items and claims.
     */
    public void deleteAccount(String userId, String name, String password) {
        User user = getUserEntity(userId);

        if (name == null || !name.trim().equalsIgnoreCase(user.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Name does not match account");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Incorrect password");
        }

        itemRepository.findByOwnerId(userId).forEach(item ->
                claimRepository.findByItemId(item.getId())
                        .forEach(claimRepository::delete));
        itemRepository.findByOwnerId(userId).forEach(itemRepository::delete);
        claimRepository.findByClaimantId(userId).forEach(claimRepository::delete);
        userRepository.delete(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
