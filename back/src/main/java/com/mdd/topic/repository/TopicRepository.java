package com.mdd.topic.repository;

import com.mdd.topic.domain.Topic;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for topics.
 */
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    /**
     * Returns the topic catalog in display order.
     */
    List<Topic> findAllByOrderByNameAsc();

    /**
     * Returns one page of the topic catalog in display order.
     */
    Page<Topic> findAllByOrderByNameAsc(Pageable pageable);
}
