package com.mdd.user.dto;

import java.util.UUID;

/**
 * Topic subscription displayed inside the user profile.
 */
public record SubscriptionResponse(
        UUID id,
        String name,
        String description
) {
}
