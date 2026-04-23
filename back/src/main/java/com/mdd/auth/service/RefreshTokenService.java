package com.mdd.auth.service;

import com.mdd.auth.domain.RefreshToken;
import com.mdd.auth.domain.User;
import com.mdd.auth.exception.InvalidRefreshTokenException;
import com.mdd.auth.exception.SuspiciousRefreshTokenReuseException;
import com.mdd.auth.repository.RefreshTokenRepository;
import com.mdd.security.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, rotates and revokes opaque refresh tokens stored as hashes.
 */
@Service
public class RefreshTokenService {
    private static final int TOKEN_BYTES = 64;
    private static final Duration ROTATION_REUSE_GRACE_PERIOD = Duration.ofSeconds(5);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    /**
     * Creates a new persisted refresh-token record and returns the one-time raw token.
     *
     * <p>Only a SHA-256 hash is stored in the database. The raw value is returned once
     * so the controller can put it in an HttpOnly cookie.</p>
     *
     * @param user user who owns the refresh-token session
     * @param request current request, used to capture coarse device metadata
     * @return raw refresh token to send to the client
     */
    @Transactional
    public String createFor(User user, HttpServletRequest request) {
        Instant now = Instant.now(clock);
        String rawToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken(
                user,
                hash(rawToken),
                now.plusMillis(jwtProperties.refreshExpiration()),
                deviceName(request),
                truncate(header(request, "User-Agent"), 255),
                now
        );
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Revokes the submitted refresh token and creates its replacement.
     *
     * <p>Refresh-token rotation limits replay attacks: a token should be usable only
     * once. Reusing an already revoked token is treated as suspicious and all active
     * sessions for the same user are revoked.</p>
     *
     * @param rawRefreshToken raw token received from the client
     * @param request current request, used to describe the replacement token device
     * @return user and replacement raw refresh token
     */
    @Transactional
    public RefreshTokenRotation rotate(String rawRefreshToken, HttpServletRequest request) {
        String tokenHash = hash(rawRefreshToken);

        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElse(null);

        if (existingToken == null) {
            throw new InvalidRefreshTokenException();
        }

        if (existingToken.isRevoked()) {
            Instant revokedAt = existingToken.getRevokedAt();
            if (revokedAt != null && !revokedAt.isBefore(Instant.now(clock).minus(ROTATION_REUSE_GRACE_PERIOD))) {
                // Concurrent refresh attempts from the same session can briefly reuse the previous token.
                // Treat this as an invalid token, not as suspicious theft.
                throw new InvalidRefreshTokenException();
            }
            // A revoked token appearing again may mean a stolen token was replayed.
            // Revoking the user's remaining active tokens forces a clean login.
            revokeActiveTokensFor(existingToken.getUser());
            throw new SuspiciousRefreshTokenReuseException();
        }

        Instant now = Instant.now(clock);
        if (!existingToken.isUsable(now)) {
            throw new InvalidRefreshTokenException();
        }

        existingToken.revoke(now);
        String nextRefreshToken = createFor(existingToken.getUser(), request);
        return new RefreshTokenRotation(existingToken.getUser(), nextRefreshToken);
    }

    /**
     * Revokes a single refresh token when it is currently usable.
     *
     * @param rawRefreshToken raw token received from the client
     */
    @Transactional
    public void revoke(String rawRefreshToken) {
        String tokenHash = hash(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElse(null);

        if (refreshToken == null) {
            throw new InvalidRefreshTokenException();
        }

        Instant now = Instant.now(clock);
        if (refreshToken.isUsable(now)) {
            refreshToken.revoke(now);
            return;
        }
    }

    /**
     * Revokes every non-revoked refresh token for a user after suspicious token reuse.
     */
    private void revokeActiveTokensFor(User user) {
        Instant now = Instant.now(clock);
        for (RefreshToken refreshToken : refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user)) {
            refreshToken.revoke(now);
        }
    }

    /**
     * Generates a high-entropy URL-safe token suitable for cookie transport.
     */
    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashes a raw refresh token before lookup or persistence.
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /**
     * Derives a short device label from the user-agent header.
     */
    private String deviceName(HttpServletRequest request) {
        String userAgent = header(request, "User-Agent");
        return userAgent == null || userAgent.isBlank() ? null : truncate(userAgent, 100);
    }

    /**
     * Reads a header while allowing tests and service calls to pass a null request.
     */
    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    /**
     * Truncates request metadata to fit database column sizes.
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
