package com.mdd.post.exception;

import java.util.UUID;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(UUID postId) {
        super("Article introuvable: " + postId);
    }
}
