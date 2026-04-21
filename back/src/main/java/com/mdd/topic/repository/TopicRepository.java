package com.mdd.topic.repository;

import com.mdd.topic.domain.Topic;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findAllByOrderByNameAsc();
}
