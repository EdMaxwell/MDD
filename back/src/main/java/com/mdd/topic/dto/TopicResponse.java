package com.mdd.topic.dto;

import java.util.UUID;

/**
 * Topic catalog item enriched with the current user's subscription state.
 */
public record TopicResponse(
        UUID id,
        String name,
        String description,
        boolean subscribed
) {
}
