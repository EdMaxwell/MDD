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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for feed sorting and feed item mapping.
 */
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

        when(postRepository.findFeedByUserSubscriptions(eq(user.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        var feed = postFeedService.currentUserFeed(user, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(postRepository).findFeedByUserSubscriptions(eq(user.getId()), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(6);
        assertThat(feed.content()).hasSize(1);
        assertThat(feed.content().getFirst().title()).isEqualTo("Article recent");
        assertThat(feed.content().getFirst().authorName()).isEqualTo("Alice");
        assertThat(feed.content().getFirst().topicName()).isEqualTo("Spring");
    }

    @Test
    void currentUserFeedShouldAcceptAscendingSort() {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", UUID.fromString("10000000-0000-0000-0000-000000000001"));
        when(postRepository.findFeedByUserSubscriptions(eq(user.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        postFeedService.currentUserFeed(user, "asc", 2, 4);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(postRepository).findFeedByUserSubscriptions(eq(user.getId()), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(4);
    }

    @Test
    void currentUserFeedShouldCapPageSizeToMaximumAllowedSize() {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", UUID.fromString("10000000-0000-0000-0000-000000000001"));
        when(postRepository.findFeedByUserSubscriptions(eq(user.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        postFeedService.currentUserFeed(user, "desc", -3, 30);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(postRepository).findFeedByUserSubscriptions(eq(user.getId()), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(24);
    }

    private Post buildPost(User user, String topicName, String title, Instant createdAt) {
        Topic topic = new Topic(topicName, "Description");
        ReflectionTestUtils.setField(topic, "id", UUID.fromString("20000000-0000-0000-0000-000000000001"));
        Post post = new Post(user, topic, title, "Contenu de test", createdAt);
        ReflectionTestUtils.setField(post, "id", UUID.fromString("40000000-0000-0000-0000-000000000001"));
        return post;
    }
}
