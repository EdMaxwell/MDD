package com.mdd.post.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Complete article representation, including author, topic and comments.
 */
public record PostDetailResponse(
        UUID id,
        String title,
        String content,
        Instant createdAt,
        UUID authorId,
        String authorName,
        UUID topicId,
        String topicName,
        List<CommentResponse> comments
) {
}
