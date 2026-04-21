package com.mdd.topic.exception;

import java.util.UUID;

public class TopicNotFoundException extends RuntimeException {

    public TopicNotFoundException(UUID topicId) {
        super("Topic not found: " + topicId);
    }
}
