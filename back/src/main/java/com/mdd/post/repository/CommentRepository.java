package com.mdd.post.repository;

import com.mdd.post.domain.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for article comments.
 */
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * Loads comments for a post with authors eagerly fetched and oldest comments first.
     */
    @EntityGraph(attributePaths = "author")
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);
}
