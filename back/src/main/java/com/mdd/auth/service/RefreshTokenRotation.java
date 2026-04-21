package com.mdd.auth.service;

import com.mdd.auth.domain.User;

public record RefreshTokenRotation(
        User user,
        String refreshToken
) {
}
