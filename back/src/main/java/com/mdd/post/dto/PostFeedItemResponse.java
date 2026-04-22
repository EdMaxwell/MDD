package com.mdd.post.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Compact article representation used by the authenticated feed.
 */
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
