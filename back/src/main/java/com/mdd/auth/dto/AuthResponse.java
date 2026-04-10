package com.mdd.auth.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
