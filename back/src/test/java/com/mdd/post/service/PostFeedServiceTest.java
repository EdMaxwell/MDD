package com.mdd.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mdd.auth.domain.User;
import com.mdd.post.domain.Post;
import com.mdd.post.repository.PostRepository;
import com.mdd.topic.domain.Topic;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostFeedServiceTest {

    @Mock
    private PostRepository postRepository;

    private PostFeedService postFeedService;

    @BeforeEach
    void setUp() {
        postFeedService = new PostFeedService(postRepository);
    }

    @Test
    void currentUserFeedShouldUseDescendingSortByDefault() {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", UUID.fromString("10000000-0000-0000-0000-000000000001"));
        Post post = buildPost(user, "Spring", "Article recent", Instant.parse("2026-04-21T10:00:00Z"));

        when(postRepository.findFeedByUserSubscriptions(eq(user.getId()), any(Sort.class))).thenReturn(List.of(post));

        var feed = postFeedService.currentUserFeed(user, null);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        org.mockito.Mockito.verify(postRepository).findFeedByUserSubscriptions(eq(user.getId()), sortCaptor.capture());

        assertThat(sortCaptor.getValue().getOrderFor("createdAt")).isNotNull();
        assertThat(sortCaptor.getValue().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(feed).hasSize(1);
        assertThat(feed.getFirst().title()).isEqualTo("Article recent");
        assertThat(feed.getFirst().authorName()).isEqualTo("Alice");
        assertThat(feed.getFirst().topicName()).isEqualTo("Spring");
    }

    @Test
    void currentUserFeedShouldAcceptAscendingSort() {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", UUID.fromString("10000000-0000-0000-0000-000000000001"));
        when(postRepository.findFeedByUserSubscriptions(eq(user.getId()), any(Sort.class))).thenReturn(List.of());

        postFeedService.currentUserFeed(user, "asc");

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        org.mockito.Mockito.verify(postRepository).findFeedByUserSubscriptions(eq(user.getId()), sortCaptor.capture());

        assertThat(sortCaptor.getValue().getOrderFor("createdAt")).isNotNull();
        assertThat(sortCaptor.getValue().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    private Post buildPost(User user, String topicName, String title, Instant createdAt) {
        Topic topic = new Topic(topicName, "Description");
        ReflectionTestUtils.setField(topic, "id", UUID.fromString("20000000-0000-0000-0000-000000000001"));
        Post post = new Post(user, topic, title, "Contenu de test", createdAt);
        ReflectionTestUtils.setField(post, "id", UUID.fromString("40000000-0000-0000-0000-000000000001"));
        return post;
    }
}
