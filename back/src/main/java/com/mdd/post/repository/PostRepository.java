package com.mdd.post.repository;

import com.mdd.post.domain.Post;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for posts and feed queries.
 */
public interface PostRepository extends JpaRepository<Post, UUID> {

    /**
     * Loads a post with author and topic to safely map API responses inside a transaction.
     */
    @EntityGraph(attributePaths = {"author", "topic"})
    Optional<Post> findWithAuthorAndTopicById(UUID id);

    /**
     * Returns posts whose topics are followed by the given user.
     *
     * <p>The entity graph prevents N+1 queries while mapping author and topic names.</p>
     */
    @EntityGraph(attributePaths = {"author", "topic"})
    @Query("""
            select post
            from Post post
            where post.topic.id in (
                select topic.id
                from User user
                join user.subscriptions topic
                where user.id = :userId
            )
            """)
    Page<Post> findFeedByUserSubscriptions(@Param("userId") UUID userId, Pageable pageable);
}
