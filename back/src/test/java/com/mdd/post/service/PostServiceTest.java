package com.mdd.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mdd.auth.domain.User;
import com.mdd.auth.repository.UserRepository;
import com.mdd.post.domain.Comment;
import com.mdd.post.domain.Post;
import com.mdd.post.dto.CreateCommentRequest;
import com.mdd.post.dto.CreatePostRequest;
import com.mdd.post.exception.PostNotFoundException;
import com.mdd.post.repository.CommentRepository;
import com.mdd.post.repository.PostRepository;
import com.mdd.topic.domain.Topic;
import com.mdd.topic.exception.TopicNotFoundException;
import com.mdd.topic.repository.TopicRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for article creation, detail loading and comment creation.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-21T12:00:00Z");

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private UserRepository userRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(
                postRepository,
                commentRepository,
                topicRepository,
                userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createShouldPersistPostWithAuthenticatedAuthorAndTopic() {
        User user = user();
        Topic topic = topic("20000000-0000-0000-0000-000000000001", "Angular");
        CreatePostRequest request = new CreatePostRequest(topic.getId(), "  Nouveau titre  ", "  Nouveau contenu  ");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(topicRepository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", UUID.fromString("40000000-0000-0000-0000-000000000001"));
            return post;
        });

        var response = postService.create(user, request);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());

        assertThat(postCaptor.getValue().getAuthor()).isEqualTo(user);
        assertThat(postCaptor.getValue().getTopic()).isEqualTo(topic);
        assertThat(postCaptor.getValue().getTitle()).isEqualTo("Nouveau titre");
        assertThat(postCaptor.getValue().getContent()).isEqualTo("Nouveau contenu");
        assertThat(postCaptor.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(response.title()).isEqualTo("Nouveau titre");
        assertThat(response.comments()).isEmpty();
    }

    @Test
    void createShouldRejectUnknownTopic() {
        User user = user();
        UUID topicId = UUID.fromString("20000000-0000-0000-0000-000000000001");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(topicRepository.findById(topicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.create(user, new CreatePostRequest(topicId, "Titre", "Contenu")))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void findByIdShouldReturnPostDetailWithOrderedComments() {
        User user = user();
        Topic topic = topic("20000000-0000-0000-0000-000000000001", "Spring");
        Post post = post(user, topic);
        Comment comment = comment(post, user, "Merci pour l'article");

        when(postRepository.findWithAuthorAndTopicById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId())).thenReturn(List.of(comment));

        var response = postService.findById(post.getId());

        assertThat(response.id()).isEqualTo(post.getId());
        assertThat(response.authorName()).isEqualTo("Alice");
        assertThat(response.topicName()).isEqualTo("Spring");
        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().getFirst().content()).isEqualTo("Merci pour l'article");
    }

    @Test
    void findByIdShouldRejectUnknownPost() {
        UUID postId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        when(postRepository.findWithAuthorAndTopicById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.findById(postId)).isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void addCommentShouldPersistCommentOnPost() {
        User user = user();
        Topic topic = topic("20000000-0000-0000-0000-000000000001", "Spring");
        Post post = post(user, topic);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", UUID.fromString("50000000-0000-0000-0000-000000000001"));
            return comment;
        });

        var response = postService.addComment(user, post.getId(), new CreateCommentRequest("  Tres utile  "));

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());

        assertThat(commentCaptor.getValue().getPost()).isEqualTo(post);
        assertThat(commentCaptor.getValue().getAuthor()).isEqualTo(user);
        assertThat(commentCaptor.getValue().getContent()).isEqualTo("Tres utile");
        assertThat(commentCaptor.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(response.content()).isEqualTo("Tres utile");
    }

    private User user() {
        User user = new User("Alice", "alice@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", UUID.fromString("10000000-0000-0000-0000-000000000001"));
        return user;
    }

    private Topic topic(String id, String name) {
        Topic topic = new Topic(name, "Description");
        ReflectionTestUtils.setField(topic, "id", UUID.fromString(id));
        return topic;
    }

    private Post post(User user, Topic topic) {
        Post post = new Post(user, topic, "Titre", "Contenu", NOW);
        ReflectionTestUtils.setField(post, "id", UUID.fromString("40000000-0000-0000-0000-000000000001"));
        return post;
    }

    private Comment comment(Post post, User user, String content) {
        Comment comment = new Comment(post, user, content, NOW);
        ReflectionTestUtils.setField(comment, "id", UUID.fromString("50000000-0000-0000-0000-000000000001"));
        return comment;
    }
}
