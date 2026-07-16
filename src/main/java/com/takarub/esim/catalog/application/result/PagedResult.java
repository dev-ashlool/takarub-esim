package com.takarub.esim.catalog.application.result;

import java.util.List;

/**
 * Generic paginated result container for read-side queries.
 */
public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
