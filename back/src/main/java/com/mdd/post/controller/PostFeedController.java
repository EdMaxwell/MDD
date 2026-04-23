package com.mdd.post.controller;

import com.mdd.auth.domain.User;
import com.mdd.common.PageResponse;
import com.mdd.post.dto.CommentResponse;
import com.mdd.post.dto.CreateCommentRequest;
import com.mdd.post.dto.CreatePostRequest;
import com.mdd.post.dto.PostDetailResponse;
import com.mdd.post.dto.PostFeedItemResponse;
import com.mdd.post.service.PostFeedService;
import com.mdd.post.service.PostService;
import jakarta.validation.Valid;
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

/**
 * Exposes authenticated post feed, post creation, detail and comment endpoints.
 */
@RestController
@RequestMapping("/api/posts")
public class PostFeedController {

    private final PostFeedService postFeedService;
    private final PostService postService;

    public PostFeedController(PostFeedService postFeedService, PostService postService) {
        this.postFeedService = postFeedService;
        this.postService = postService;
    }

    /**
     * Creates an article for the authenticated user.
     *
     * @param user principal resolved from the access token
     * @param request validated article creation payload
     * @return created article detail
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return postService.create(user, request);
    }

    /**
     * Returns the authenticated user's feed from followed topics.
     *
     * @param user principal resolved from the access token
     * @param sort requested creation-date order
     * @param page zero-based page index
     * @param size requested page size, capped by the backend
     * @return paginated feed items
     */
    @GetMapping("/feed")
    public PageResponse<PostFeedItemResponse> feed(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return postFeedService.currentUserFeed(user, sort, page, size);
    }

    /**
     * Returns the detail of one article.
     *
     * @param postId article identifier from the route
     * @return article detail including comments
     */
    @GetMapping("/{postId}")
    public PostDetailResponse detail(@PathVariable UUID postId) {
        return postService.findById(postId);
    }

    /**
     * Adds a comment to an article for the authenticated user.
     *
     * @param user principal resolved from the access token
     * @param postId article identifier from the route
     * @param request validated comment payload
     * @return created comment
     */
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
