package com.mdd.user.exception;

import java.util.UUID;

/**
 * Raised when a user cannot be found.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }
}
