package com.mdd.post.controller;

import com.mdd.auth.domain.User;
import com.mdd.post.dto.PostFeedItemResponse;
import com.mdd.post.service.PostFeedService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostFeedController {

    private final PostFeedService postFeedService;

    public PostFeedController(PostFeedService postFeedService) {
        this.postFeedService = postFeedService;
    }

    @GetMapping("/feed")
    public List<PostFeedItemResponse> feed(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "desc") String sort
    ) {
        return postFeedService.currentUserFeed(user, sort);
    }
}
