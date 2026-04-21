package com.mdd.post.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String content,
        Instant createdAt,
        UUID authorId,
        String authorName
) {
}
