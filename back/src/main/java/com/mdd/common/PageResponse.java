package com.mdd.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable API pagination contract that avoids exposing Spring Page internals.
 *
 * @param content current page items
 * @param page zero-based page index
 * @param size requested page size after backend bounds are applied
 * @param totalElements total matching items
 * @param totalPages total available pages
 * @param first whether this is the first page
 * @param last whether this is the last page
 * @param <T> item type
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Converts a Spring Data page to the public pagination response.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
