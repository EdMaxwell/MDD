package com.mdd.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credentials submitted by a user who wants to open a session.
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
