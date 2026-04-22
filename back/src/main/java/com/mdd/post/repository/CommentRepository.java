package com.mdd.post.repository;

import com.mdd.post.domain.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);
}
