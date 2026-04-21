package com.mdd.post.controller;

import com.mdd.auth.domain.User;
import com.mdd.post.dto.CommentResponse;
import com.mdd.post.dto.CreateCommentRequest;
import com.mdd.post.dto.CreatePostRequest;
import com.mdd.post.dto.PostDetailResponse;
import com.mdd.post.dto.PostFeedItemResponse;
import com.mdd.post.service.PostFeedService;
import com.mdd.post.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostFeedController {

    private final PostFeedService postFeedService;
    private final PostService postService;

    public PostFeedController(PostFeedService postFeedService, PostService postService) {
        this.postFeedService = postFeedService;
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return postService.create(user, request);
    }

    @GetMapping("/feed")
    public List<PostFeedItemResponse> feed(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "desc") String sort
    ) {
        return postFeedService.currentUserFeed(user, sort);
    }

    @GetMapping("/{postId}")
    public PostDetailResponse detail(@PathVariable UUID postId) {
        return postService.findById(postId);
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(
            @AuthenticationPrincipal User user,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return postService.addComment(user, postId, request);
    }
}
