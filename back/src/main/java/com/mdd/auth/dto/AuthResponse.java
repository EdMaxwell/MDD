package com.mdd.auth.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        UserResponse user
) {

    public AuthResponse withoutRefreshToken() {
        return new AuthResponse(token, null, user);
    }
}
