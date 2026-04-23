package com.mdd.topic.service;

import com.mdd.auth.domain.User;
import com.mdd.auth.repository.UserRepository;
import com.mdd.common.PageResponse;
import com.mdd.topic.domain.Topic;
import com.mdd.topic.dto.TopicResponse;
import com.mdd.topic.exception.TopicNotFoundException;
import com.mdd.topic.repository.TopicRepository;
import com.mdd.user.exception.UserNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the topic catalog view and the authenticated user's subscriptions.
 */
@Service
public class TopicSubscriptionService {

    public static final int DEFAULT_PAGE_SIZE = 6;
    public static final int MAX_PAGE_SIZE = 24;

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public TopicSubscriptionService(TopicRepository topicRepository, UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lists every topic and marks whether the authenticated user follows each one.
     *
     * @param authenticatedUser principal resolved by Spring Security
     * @return topic catalog enriched with subscription state
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> listTopicsFor(User authenticatedUser) {
        User user = findUserWithSubscriptions(authenticatedUser);
        Set<UUID> subscribedTopicIds = subscribedTopicIds(user);

        return topicRepository.findAllByOrderByNameAsc().stream()
                .map(topic -> toResponse(topic, subscribedTopicIds.contains(topic.getId())))
                .toList();
    }

    /**
     * Lists one page of topics and marks whether the authenticated user follows each one.
     *
     * @param authenticatedUser principal resolved by Spring Security
     * @param page zero-based page index
     * @param size requested page size, capped at {@value #MAX_PAGE_SIZE}
     * @return paginated topic catalog enriched with subscription state
     */
    @Transactional(readOnly = true)
    public PageResponse<TopicResponse> listTopicsFor(User authenticatedUser, Integer page, Integer size) {
        User user = findUserWithSubscriptions(authenticatedUser);
        Set<UUID> subscribedTopicIds = subscribedTopicIds(user);
        Pageable pageable = PageRequest.of(sanitizePage(page), sanitizeSize(size));

        return PageResponse.from(topicRepository.findAllByOrderByNameAsc(pageable)
                .map(topic -> toResponse(topic, subscribedTopicIds.contains(topic.getId()))));
    }

    /**
     * Keeps invalid page values on the first page instead of returning a request error.
     */
    private int sanitizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    /**
     * Applies a maximum topic catalog page size to keep requests bounded.
     */
    private int sanitizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * Subscribes the authenticated user to a topic.
     *
     * <p>The operation is naturally idempotent because the relation is backed by a {@link Set}.</p>
     *
     * @param authenticatedUser principal resolved by Spring Security
     * @param topicId topic identifier from the route
     * @return topic response marked as subscribed
     */
    @Transactional
    public TopicResponse subscribe(User authenticatedUser, UUID topicId) {
        User user = findUserWithSubscriptions(authenticatedUser);
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));

        user.getSubscriptions().add(topic);
        userRepository.save(user);

        return toResponse(topic, true);
    }

    /**
     * Removes a topic subscription for the authenticated user.
     *
     * <p>Removing an absent topic from the set is harmless, keeping the operation idempotent.</p>
     *
     * @param authenticatedUser principal resolved by Spring Security
     * @param topicId topic identifier from the route
     * @return topic response marked as not subscribed
     */
    @Transactional
    public TopicResponse unsubscribe(User authenticatedUser, UUID topicId) {
        User user = findUserWithSubscriptions(authenticatedUser);
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));

        user.getSubscriptions().remove(topic);
        userRepository.save(user);

        return toResponse(topic, false);
    }

    /**
     * Reloads the principal with subscriptions to avoid lazy-loading outside the transaction.
     */
    private User findUserWithSubscriptions(User authenticatedUser) {
        return userRepository.findWithSubscriptionsById(authenticatedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getId()));
    }

    /**
     * Creates an identifier set so subscription checks stay O(1) while mapping all topics.
     */
    private Set<UUID> subscribedTopicIds(User user) {
        Set<UUID> topicIds = new HashSet<>();
        user.getSubscriptions().forEach(topic -> topicIds.add(topic.getId()));
        return topicIds;
    }

    /**
     * Maps a topic entity to the catalog API contract.
     */
    private TopicResponse toResponse(Topic topic, boolean subscribed) {
        return new TopicResponse(topic.getId(), topic.getName(), topic.getDescription(), subscribed);
    }
}
