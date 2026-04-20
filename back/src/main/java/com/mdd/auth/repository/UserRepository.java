package com.mdd.auth.repository;

import com.mdd.auth.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "subscriptions")
    Optional<User> findWithSubscriptionsById(UUID id);
}
