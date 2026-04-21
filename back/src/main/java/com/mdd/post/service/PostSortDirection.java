package com.mdd.post.service;

import java.util.Locale;
import org.springframework.data.domain.Sort;

public enum PostSortDirection {
    ASC,
    DESC;

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

    Sort toSort() {
        return Sort.by(this == ASC ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
    }
}
