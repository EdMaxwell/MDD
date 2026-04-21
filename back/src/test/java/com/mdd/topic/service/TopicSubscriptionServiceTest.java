package com.mdd.topic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mdd.auth.domain.User;
import com.mdd.auth.repository.UserRepository;
import com.mdd.topic.domain.Topic;
import com.mdd.topic.exception.TopicNotFoundException;
import com.mdd.topic.repository.TopicRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TopicSubscriptionServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private UserRepository userRepository;

    private TopicSubscriptionService topicSubscriptionService;

    @BeforeEach
    void setUp() {
        topicSubscriptionService = new TopicSubscriptionService(topicRepository, userRepository);
    }

    @Test
    void listTopicsForShouldReturnAllTopicsWithSubscriptionState() {
        User authenticatedUser = user();
        Topic angular = topic("00000000-0000-0000-0000-000000000011", "Angular", "Framework frontend");
        Topic spring = topic("00000000-0000-0000-0000-000000000012", "Spring", "Framework Java");
        authenticatedUser.getSubscriptions().add(angular);

        when(userRepository.findWithSubscriptionsById(authenticatedUser.getId())).thenReturn(Optional.of(authenticatedUser));
        when(topicRepository.findAllByOrderByNameAsc()).thenReturn(List.of(angular, spring));

        var topics = topicSubscriptionService.listTopicsFor(authenticatedUser);

        assertThat(topics).hasSize(2);
        assertThat(topics.getFirst().name()).isEqualTo("Angular");
        assertThat(topics.getFirst().subscribed()).isTrue();
        assertThat(topics.get(1).name()).isEqualTo("Spring");
        assertThat(topics.get(1).subscribed()).isFalse();
    }

    @Test
    void subscribeShouldAddTopicToUserSubscriptions() {
        User authenticatedUser = user();
        Topic angular = topic("00000000-0000-0000-0000-000000000011", "Angular", "Framework frontend");

        when(userRepository.findWithSubscriptionsById(authenticatedUser.getId())).thenReturn(Optional.of(authenticatedUser));
        when(topicRepository.findById(angular.getId())).thenReturn(Optional.of(angular));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = topicSubscriptionService.subscribe(authenticatedUser, angular.getId());

        assertThat(response.subscribed()).isTrue();
        assertThat(authenticatedUser.getSubscriptions()).containsExactly(angular);
        verify(userRepository).save(authenticatedUser);
    }

    @Test
    void unsubscribeShouldRemoveTopicFromUserSubscriptions() {
        User authenticatedUser = user();
        Topic angular = topic("00000000-0000-0000-0000-000000000011", "Angular", "Framework frontend");
        authenticatedUser.getSubscriptions().add(angular);

        when(userRepository.findWithSubscriptionsById(authenticatedUser.getId())).thenReturn(Optional.of(authenticatedUser));
        when(topicRepository.findById(angular.getId())).thenReturn(Optional.of(angular));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = topicSubscriptionService.unsubscribe(authenticatedUser, angular.getId());

        assertThat(response.subscribed()).isFalse();
        assertThat(authenticatedUser.getSubscriptions()).isEmpty();
        verify(userRepository).save(authenticatedUser);
    }

    @Test
    void subscribeShouldRejectUnknownTopic() {
        User authenticatedUser = user();
        UUID topicId = UUID.fromString("00000000-0000-0000-0000-000000000011");

        when(userRepository.findWithSubscriptionsById(authenticatedUser.getId())).thenReturn(Optional.of(authenticatedUser));
        when(topicRepository.findById(topicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicSubscriptionService.subscribe(authenticatedUser, topicId))
                .isInstanceOf(TopicNotFoundException.class);
    }

    private User user() {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return user;
    }

    private Topic topic(String id, String name, String description) {
        Topic topic = new Topic(name, description);
        ReflectionTestUtils.setField(topic, "id", UUID.fromString(id));
        return topic;
    }
}
