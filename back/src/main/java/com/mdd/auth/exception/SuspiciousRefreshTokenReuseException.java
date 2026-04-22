package com.mdd.auth.exception;

/**
 * Raised when a revoked refresh token is presented again and session reuse is suspected.
 */
public class SuspiciousRefreshTokenReuseException extends RuntimeException {

    public SuspiciousRefreshTokenReuseException() {
        super("Suspicious refresh token reuse detected");
    }
}
