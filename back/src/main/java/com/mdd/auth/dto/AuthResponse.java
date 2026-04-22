package com.mdd.auth.dto;

/**
 * Authentication payload returned after login, registration or token refresh.
 */
public record AuthResponse(
        String token,
        String refreshToken,
        UserResponse user
) {

    public AuthResponse withoutRefreshToken() {
        return new AuthResponse(token, null, user);
    }
}
