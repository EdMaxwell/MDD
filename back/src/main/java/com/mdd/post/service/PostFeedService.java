package com.mdd.post.service;

import com.mdd.auth.domain.User;
import com.mdd.post.domain.Post;
import com.mdd.post.dto.PostFeedItemResponse;
import com.mdd.post.repository.PostRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostFeedService {

    private final PostRepository postRepository;

    public PostFeedService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<PostFeedItemResponse> currentUserFeed(User user, String sortDirection) {
        PostSortDirection direction = PostSortDirection.from(sortDirection);
        return postRepository.findFeedByUserSubscriptions(user.getId(), direction.toSort()).stream()
                .map(this::toFeedItem)
                .toList();
    }

    private PostFeedItemResponse toFeedItem(Post post) {
        return new PostFeedItemResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.getTopic().getId(),
                post.getTopic().getName()
        );
    }
}
