package com.mdd.auth.service;

import com.mdd.auth.domain.User;

/**
 * Result of a successful refresh-token rotation.
 */
public record RefreshTokenRotation(
        User user,
        String refreshToken
) {
}
