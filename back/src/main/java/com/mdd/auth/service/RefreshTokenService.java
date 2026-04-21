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
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public String createFor(User user, HttpServletRequest request) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken(
                user,
                hash(rawToken),
                Instant.now().plusMillis(jwtProperties.refreshExpiration()),
                deviceName(request),
                truncate(header(request, "User-Agent"), 255)
        );
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public RefreshTokenRotation rotate(String rawRefreshToken, HttpServletRequest request) {
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existingToken.isRevoked()) {
            revokeActiveTokensFor(existingToken.getUser());
            LOGGER.warn("Suspicious reuse of revoked refresh token for user {}", existingToken.getUser().getId());
            throw new SuspiciousRefreshTokenReuseException();
        }

        if (!existingToken.isUsable(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        existingToken.revoke();
        String nextRefreshToken = createFor(existingToken.getUser(), request);
        return new RefreshTokenRotation(existingToken.getUser(), nextRefreshToken);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.isUsable(Instant.now())) {
            refreshToken.revoke();
        }
    }

    private void revokeActiveTokensFor(User user) {
        for (RefreshToken refreshToken : refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user)) {
            refreshToken.revoke();
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String deviceName(HttpServletRequest request) {
        String userAgent = header(request, "User-Agent");
        return userAgent == null || userAgent.isBlank() ? null : truncate(userAgent, 100);
    }

    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
