package com.mdd.auth.exception;

/**
 * Raised when an email address already belongs to another account.
 */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("An account already exists for email " + email);
    }
}
