package com.mdd.post.service;

import com.mdd.auth.domain.User;
import com.mdd.post.domain.Post;
import com.mdd.post.dto.PostFeedItemResponse;
import com.mdd.post.repository.PostRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the authenticated user's post feed from their topic subscriptions.
 */
@Service
public class PostFeedService {

    private final PostRepository postRepository;

    public PostFeedService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * Returns posts from topics followed by the authenticated user.
     *
     * @param user authenticated principal
     * @param sortDirection accepted values are defined by {@link PostSortDirection#from(String)}
     * @return feed items sorted by creation date
     */
    @Transactional(readOnly = true)
    public List<PostFeedItemResponse> currentUserFeed(User user, String sortDirection) {
        PostSortDirection direction = PostSortDirection.from(sortDirection);
        return postRepository.findFeedByUserSubscriptions(user.getId(), direction.toSort()).stream()
                .map(this::toFeedItem)
                .toList();
    }

    /**
     * Maps a post entity with author and topic already fetched to the feed API contract.
     */
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
