package com.mdd.post.service;

import com.mdd.auth.domain.User;
import com.mdd.auth.repository.UserRepository;
import com.mdd.post.domain.Comment;
import com.mdd.post.domain.Post;
import com.mdd.post.dto.CommentResponse;
import com.mdd.post.dto.CreateCommentRequest;
import com.mdd.post.dto.CreatePostRequest;
import com.mdd.post.dto.PostDetailResponse;
import com.mdd.post.exception.PostNotFoundException;
import com.mdd.post.repository.CommentRepository;
import com.mdd.post.repository.PostRepository;
import com.mdd.topic.domain.Topic;
import com.mdd.topic.exception.TopicNotFoundException;
import com.mdd.topic.repository.TopicRepository;
import com.mdd.user.exception.UserNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public PostService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            TopicRepository topicRepository,
            UserRepository userRepository,
            Clock clock
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public PostDetailResponse create(User authenticatedUser, CreatePostRequest request) {
        User author = findUser(authenticatedUser);
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new TopicNotFoundException(request.topicId()));
        Instant now = Instant.now(clock);

        Post post = postRepository.save(new Post(author, topic, request.title().trim(), request.content().trim(), now));

        return toDetailResponse(post, List.of());
    }

    @Transactional(readOnly = true)
    public PostDetailResponse findById(UUID postId) {
        Post post = postRepository.findWithAuthorAndTopicById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        List<CommentResponse> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toCommentResponse)
                .toList();

        return toDetailResponse(post, comments);
    }

    @Transactional
    public CommentResponse addComment(User authenticatedUser, UUID postId, CreateCommentRequest request) {
        User author = findUser(authenticatedUser);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        Instant now = Instant.now(clock);

        Comment comment = commentRepository.save(new Comment(post, author, request.content().trim(), now));

        return toCommentResponse(comment);
    }

    private User findUser(User authenticatedUser) {
        return userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getId()));
    }

    private PostDetailResponse toDetailResponse(Post post, List<CommentResponse> comments) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.getTopic().getId(),
                post.getTopic().getName(),
                comments
        );
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName()
        );
    }
}
