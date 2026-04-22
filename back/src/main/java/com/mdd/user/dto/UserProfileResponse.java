package com.mdd.user.dto;

import java.util.List;
import java.util.UUID;

/**
 * Public profile data for the authenticated user.
 */
public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        List<SubscriptionResponse> subscriptions
) {
}
