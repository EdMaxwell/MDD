package com.mdd.topic.controller;

import com.mdd.auth.domain.User;
import com.mdd.topic.dto.TopicResponse;
import com.mdd.topic.service.TopicSubscriptionService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicSubscriptionService topicSubscriptionService;

    public TopicController(TopicSubscriptionService topicSubscriptionService) {
        this.topicSubscriptionService = topicSubscriptionService;
    }

    @GetMapping
    public List<TopicResponse> topics(@AuthenticationPrincipal User user) {
        return topicSubscriptionService.listTopicsFor(user);
    }

    @PostMapping("/{topicId}/subscription")
    public TopicResponse subscribe(@AuthenticationPrincipal User user, @PathVariable UUID topicId) {
        return topicSubscriptionService.subscribe(user, topicId);
    }

    @DeleteMapping("/{topicId}/subscription")
    public TopicResponse unsubscribe(@AuthenticationPrincipal User user, @PathVariable UUID topicId) {
        return topicSubscriptionService.unsubscribe(user, topicId);
    }
}
