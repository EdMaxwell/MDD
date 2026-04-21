package com.mdd.auth.exception;

public class SuspiciousRefreshTokenReuseException extends RuntimeException {

    public SuspiciousRefreshTokenReuseException() {
        super("Suspicious refresh token reuse detected");
    }
}
