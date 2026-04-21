package com.mdd.post.repository;

import com.mdd.post.domain.Post;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @EntityGraph(attributePaths = {"author", "topic"})
    Optional<Post> findWithAuthorAndTopicById(UUID id);

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
    List<Post> findFeedByUserSubscriptions(@Param("userId") UUID userId, Sort sort);
}
