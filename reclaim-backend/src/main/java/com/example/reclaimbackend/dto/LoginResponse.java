package com.example.reclaimbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for the {@code POST /api/auth/login} endpoint.
 * <p>
 * Contains the JWT access token issued upon successful authentication.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse { private String token; }
