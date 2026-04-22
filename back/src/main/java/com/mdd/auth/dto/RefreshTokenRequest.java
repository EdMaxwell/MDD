package com.mdd.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Optional body fallback for clients that cannot rely on refresh-token cookies.
 */
public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
