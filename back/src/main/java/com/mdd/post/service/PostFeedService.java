package com.mdd.post.service;

import com.mdd.auth.domain.User;
import com.mdd.common.PageResponse;
import com.mdd.post.domain.Post;
import com.mdd.post.dto.PostFeedItemResponse;
import com.mdd.post.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the authenticated user's post feed from their topic subscriptions.
 */
@Service
public class PostFeedService {

    public static final int DEFAULT_PAGE_SIZE = 6;
    public static final int MAX_PAGE_SIZE = 24;

    private final PostRepository postRepository;

    public PostFeedService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * Returns posts from topics followed by the authenticated user.
     *
     * @param user authenticated principal
     * @param sortDirection accepted values are defined by {@link PostSortDirection#from(String)}
     * @param page zero-based page index
     * @param size requested page size, capped at {@value #MAX_PAGE_SIZE}
     * @return paginated feed items sorted by creation date
     */
    @Transactional(readOnly = true)
    public PageResponse<PostFeedItemResponse> currentUserFeed(User user, String sortDirection, Integer page, Integer size) {
        PostSortDirection direction = PostSortDirection.from(sortDirection);
        Pageable pageable = PageRequest.of(sanitizePage(page), sanitizeSize(size), direction.toSort());

        return PageResponse.from(postRepository.findFeedByUserSubscriptions(user.getId(), pageable)
                .map(this::toFeedItem));
    }

    /**
     * Keeps invalid page values on the first page instead of returning a request error.
     */
    private int sanitizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    /**
     * Applies the feed page-size policy so API consumers cannot request an unbounded list.
     */
    private int sanitizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
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
