package com.mdd.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores a hashed refresh token and its revocation metadata for one user session.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "user_agent")
    private String userAgent;

    protected RefreshToken() {
    }

    /**
     * Creates a refresh-token record from a precomputed token hash.
     */
    public RefreshToken(
            User user,
            String tokenHash,
            Instant expiresAt,
            String deviceName,
            String userAgent,
            Instant createdAt
    ) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.deviceName = deviceName;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /**
     * Returns the user who owns this refresh-token session.
     */
    public User getUser() {
        return user;
    }

    /**
     * Checks whether the token can still be rotated or revoked.
     *
     * @param now current time from the application clock
     * @return true when the token is not revoked and has not expired
     */
    public boolean isUsable(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    /**
     * Indicates whether this token was already revoked.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Marks the token as revoked once and updates its modification timestamp.
     *
     * @param now revocation time from the application clock
     */
    public void revoke(Instant now) {
        if (revokedAt != null) {
            return;
        }
        this.revokedAt = now;
        this.updatedAt = now;
    }
}
