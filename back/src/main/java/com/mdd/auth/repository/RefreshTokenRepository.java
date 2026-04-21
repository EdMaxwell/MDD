package com.mdd.auth.repository;

import com.mdd.auth.domain.RefreshToken;
import com.mdd.auth.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Iterable<RefreshToken> findAllByUserAndRevokedAtIsNull(User user);
}
