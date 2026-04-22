package com.mdd.auth.dto;

import java.util.UUID;

/**
 * Public representation of an authenticated user.
 */
public record UserResponse(
        UUID id,
        String name,
        String email
) {
}
