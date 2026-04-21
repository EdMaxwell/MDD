package com.mdd.user.dto;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        List<SubscriptionResponse> subscriptions
) {
}
