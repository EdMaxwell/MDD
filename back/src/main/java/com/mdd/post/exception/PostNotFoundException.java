package com.mdd.post.exception;

import java.util.UUID;

/**
 * Raised when an article cannot be found.
 */
public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(UUID postId) {
        super("Article introuvable: " + postId);
    }
}
