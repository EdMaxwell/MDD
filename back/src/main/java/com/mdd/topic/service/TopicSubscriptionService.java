package com.mdd.topic.service;

import com.mdd.auth.domain.User;
import com.mdd.auth.repository.UserRepository;
import com.mdd.topic.domain.Topic;
import com.mdd.topic.dto.TopicResponse;
import com.mdd.topic.exception.TopicNotFoundException;
import com.mdd.topic.repository.TopicRepository;
import com.mdd.user.exception.UserNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicSubscriptionService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public TopicSubscriptionService(TopicRepository topicRepository, UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> listTopicsFor(User authenticatedUser) {
        User user = findUserWithSubscriptions(authenticatedUser);
        Set<UUID> subscribedTopicIds = subscribedTopicIds(user);

        return topicRepository.findAllByOrderByNameAsc().stream()
                .map(topic -> toResponse(topic, subscribedTopicIds.contains(topic.getId())))
                .toList();
    }

    @Transactional
    public TopicResponse subscribe(User authenticatedUser, UUID topicId) {
        User user = findUserWithSubscriptions(authenticatedUser);
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));

        user.getSubscriptions().add(topic);
        userRepository.save(user);

        return toResponse(topic, true);
    }

    @Transactional
    public TopicResponse unsubscribe(User authenticatedUser, UUID topicId) {
        User user = findUserWithSubscriptions(authenticatedUser);
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new TopicNotFoundException(topicId));

        user.getSubscriptions().remove(topic);
        userRepository.save(user);

        return toResponse(topic, false);
    }

    private User findUserWithSubscriptions(User authenticatedUser) {
        return userRepository.findWithSubscriptionsById(authenticatedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getId()));
    }

    private Set<UUID> subscribedTopicIds(User user) {
        Set<UUID> topicIds = new HashSet<>();
        user.getSubscriptions().forEach(topic -> topicIds.add(topic.getId()));
        return topicIds;
    }

    private TopicResponse toResponse(Topic topic, boolean subscribed) {
        return new TopicResponse(topic.getId(), topic.getName(), topic.getDescription(), subscribed);
    }
}
