package com.mdd.auth.exception;

/**
 * Raised when login credentials cannot authenticate a user.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
