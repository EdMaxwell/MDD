package com.mdd.user.dto;

import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        String name,
        String description
) {
}
