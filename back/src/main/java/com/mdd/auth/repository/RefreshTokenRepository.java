package com.mdd.auth.repository;

import com.mdd.auth.domain.RefreshToken;
import com.mdd.auth.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for refresh-token rotation and revocation.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Finds a persisted refresh-token session from the SHA-256 hash of the raw token.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Returns all active refresh-token records for a user so suspicious reuse can revoke them.
     */
    Iterable<RefreshToken> findAllByUserAndRevokedAtIsNull(User user);
}
