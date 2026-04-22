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
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, rotates and revokes opaque refresh tokens stored as hashes.
 */
@Service
public class RefreshTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 64;

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
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existingToken.isRevoked()) {
            // A revoked token appearing again may mean a stolen token was replayed.
            // Revoking the user's remaining active tokens forces a clean login.
            revokeActiveTokensFor(existingToken.getUser());
            LOGGER.warn("Suspicious reuse of revoked refresh token for user {}", existingToken.getUser().getId());
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
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now(clock);
        if (refreshToken.isUsable(now)) {
            refreshToken.revoke(now);
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
