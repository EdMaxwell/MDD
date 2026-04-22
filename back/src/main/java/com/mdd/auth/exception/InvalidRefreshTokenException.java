package com.mdd.auth.exception;

/**
 * Raised when a refresh token is missing, unknown, expired or unusable.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
