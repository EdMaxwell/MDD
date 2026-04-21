package com.mdd.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mdd.auth.domain.RefreshToken;
import com.mdd.auth.domain.User;
import com.mdd.auth.exception.SuspiciousRefreshTokenReuseException;
import com.mdd.auth.repository.RefreshTokenRepository;
import com.mdd.security.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                "secret",
                3600000,
                2592000000L,
                "mdd_refresh_token",
                false,
                "Lax"
        );
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtProperties);
    }

    @Test
    void rotateShouldDetectRevokedRefreshTokenReuseAndRevokeActiveUserTokens() throws Exception {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        RefreshToken reusedToken = new RefreshToken(user, hash("reused-token"), Instant.now().plusSeconds(60), null, null);
        reusedToken.revoke();
        RefreshToken activeToken = new RefreshToken(user, hash("active-token"), Instant.now().plusSeconds(60), null, null);

        when(refreshTokenRepository.findByTokenHash(hash("reused-token"))).thenReturn(Optional.of(reusedToken));
        when(refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user)).thenReturn(List.of(activeToken));

        assertThatThrownBy(() -> refreshTokenService.rotate("reused-token", null))
                .isInstanceOf(SuspiciousRefreshTokenReuseException.class);

        assertThat(activeToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).findAllByUserAndRevokedAtIsNull(user);
    }

    private String hash(String rawToken) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
