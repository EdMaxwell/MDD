package com.mdd.post.dto;

import java.time.Instant;
import java.util.UUID;

public record PostFeedItemResponse(
        UUID id,
        String title,
        String content,
        Instant createdAt,
        UUID authorId,
        String authorName,
        UUID topicId,
        String topicName
) {
}
