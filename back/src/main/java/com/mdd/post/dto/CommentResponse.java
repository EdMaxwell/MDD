package com.mdd.post.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public comment data returned in article detail responses.
 */
public record CommentResponse(
        UUID id,
        String content,
        Instant createdAt,
        UUID authorId,
        String authorName
) {
}
