package com.takarub.esim.catalog.application.usecase;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.query.SearchPackagesQuery;
import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.catalog.application.result.PagedResult;

/**
 * Read-only use case: searches available catalog packages by free-text term with pagination.
 */
public class SearchPackagesUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final CatalogBrowsePort catalogBrowsePort;

    public SearchPackagesUseCase(CatalogBrowsePort catalogBrowsePort) {
        this.catalogBrowsePort = catalogBrowsePort;
    }

    public PagedResult<CatalogPackageView> execute(SearchPackagesQuery query) {
        String term = sanitizeTerm(query.searchTerm());
        int page = Math.max(0, query.page());
        int size = Math.max(1, Math.min(query.size(), MAX_PAGE_SIZE));

        boolean hasFilters = query.countryIso() != null || query.dataAmount() != null
                || query.dataUnit() != null || query.durationDays() != null;

        if (term.isEmpty() && !hasFilters) {
            return new PagedResult<>(java.util.List.of(), page, size, 0, 0);
        }

        return catalogBrowsePort.searchAvailablePackages(
                term.isEmpty() ? null : term,
                query.countryIso(), query.dataAmount(), query.dataUnit(), query.durationDays(),
                page, size);
    }

    private static String sanitizeTerm(String searchTerm) {
        if (searchTerm == null) {
            return "";
        }
        return searchTerm.trim();
    }
}
