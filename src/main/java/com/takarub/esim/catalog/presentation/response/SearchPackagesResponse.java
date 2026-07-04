package com.takarub.esim.catalog.presentation.response;

import java.util.List;

/**
 * Paginated REST response for catalog package search results.
 */
public record SearchPackagesResponse(
        List<CatalogPackageResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
