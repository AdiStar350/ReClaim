package com.example.reclaimbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Device token used to send this user notifications.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {

    @NotBlank(message = "FCM token is required")
    private String token;
}
