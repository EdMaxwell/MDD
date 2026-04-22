package com.mdd.topic.controller;

import com.mdd.auth.domain.User;
import com.mdd.common.PageResponse;
import com.mdd.topic.dto.TopicResponse;
import com.mdd.topic.service.TopicSubscriptionService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes topic catalog and subscription endpoints for the authenticated user.
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicSubscriptionService topicSubscriptionService;

    public TopicController(TopicSubscriptionService topicSubscriptionService) {
        this.topicSubscriptionService = topicSubscriptionService;
    }

    /**
     * Lists all topics and marks the authenticated user's current subscriptions.
     *
     * @param user principal resolved from the access token
     * @param page zero-based page index
     * @param size requested page size, capped by the backend
     * @return paginated topic catalog
     */
    @GetMapping
    public PageResponse<TopicResponse> topics(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return topicSubscriptionService.listTopicsFor(user, page, size);
    }

    /**
     * Subscribes the authenticated user to a topic.
     *
     * @param user principal resolved from the access token
     * @param topicId topic identifier from the route
     * @return subscribed topic representation
     */
    @PostMapping("/{topicId}/subscription")
    public TopicResponse subscribe(@AuthenticationPrincipal User user, @PathVariable UUID topicId) {
        return topicSubscriptionService.subscribe(user, topicId);
    }

    /**
     * Unsubscribes the authenticated user from a topic.
     *
     * @param user principal resolved from the access token
     * @param topicId topic identifier from the route
     * @return unsubscribed topic representation
     */
    @DeleteMapping("/{topicId}/subscription")
    public TopicResponse unsubscribe(@AuthenticationPrincipal User user, @PathVariable UUID topicId) {
        return topicSubscriptionService.unsubscribe(user, topicId);
    }
}
