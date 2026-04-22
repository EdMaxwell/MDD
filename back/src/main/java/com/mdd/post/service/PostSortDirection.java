package com.mdd.post.service;

import java.util.Locale;
import org.springframework.data.domain.Sort;

/**
 * Supported sort directions for the article feed.
 */
public enum PostSortDirection {
    ASC,
    DESC;

    /**
     * Parses user-facing sort aliases and falls back to newest first.
     *
     * @param value requested sort direction or alias
     * @return resolved direction
     */
    public static PostSortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "asc", "oldest" -> ASC;
            case "desc", "newest" -> DESC;
            default -> DESC;
        };
    }

    /**
     * Converts the feed direction into the Spring Data sort object used by the repository.
     */
    Sort toSort() {
        return Sort.by(this == ASC ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
    }
}
