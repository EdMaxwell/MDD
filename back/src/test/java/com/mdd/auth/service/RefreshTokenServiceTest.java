package com.mdd.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mdd.auth.domain.RefreshToken;
import com.mdd.auth.domain.User;
import com.mdd.auth.exception.InvalidRefreshTokenException;
import com.mdd.auth.exception.SuspiciousRefreshTokenReuseException;
import com.mdd.auth.repository.RefreshTokenRepository;
import com.mdd.security.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for refresh-token rotation and suspicious reuse handling.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-22T08:00:00Z");

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
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                jwtProperties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void rotateShouldDetectRevokedRefreshTokenReuseAndRevokeActiveUserTokens() throws Exception {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        RefreshToken reusedToken = new RefreshToken(user, hash("reused-token"), NOW.plusSeconds(60), null, null, NOW);
        reusedToken.revoke(NOW.minusSeconds(60));
        RefreshToken activeToken = new RefreshToken(user, hash("active-token"), NOW.plusSeconds(60), null, null, NOW);

        when(refreshTokenRepository.findByTokenHash(hash("reused-token"))).thenReturn(Optional.of(reusedToken));
        when(refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user)).thenReturn(List.of(activeToken));

        assertThatThrownBy(() -> refreshTokenService.rotate("reused-token", null))
                .isInstanceOf(SuspiciousRefreshTokenReuseException.class);

        assertThat(activeToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).findAllByUserAndRevokedAtIsNull(user);
    }

    @Test
    void rotateShouldTreatVeryRecentRevokedTokenAsInvalidWithoutRevokingOtherSessions() throws Exception {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        RefreshToken reusedToken = new RefreshToken(user, hash("reused-token"), NOW.plusSeconds(60), null, null, NOW);
        reusedToken.revoke(NOW);

        when(refreshTokenRepository.findByTokenHash(hash("reused-token"))).thenReturn(Optional.of(reusedToken));

        assertThatThrownBy(() -> refreshTokenService.rotate("reused-token", null))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).findAllByUserAndRevokedAtIsNull(user);
    }

    private String hash(String rawToken) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
