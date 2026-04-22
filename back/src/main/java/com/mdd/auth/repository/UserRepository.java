package com.mdd.auth.repository;

import com.mdd.auth.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for users and their topic subscriptions.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Checks whether an email is already used, ignoring user input casing.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks email uniqueness while excluding the user currently being updated.
     */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    /**
     * Loads a user by email for authentication.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Loads a user with subscriptions eagerly for profile and topic operations.
     */
    @EntityGraph(attributePaths = "subscriptions")
    Optional<User> findWithSubscriptionsById(UUID id);
}
