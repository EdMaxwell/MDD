package com.mdd.topic.dto;

import java.util.UUID;

public record TopicResponse(
        UUID id,
        String name,
        String description,
        boolean subscribed
) {
}
