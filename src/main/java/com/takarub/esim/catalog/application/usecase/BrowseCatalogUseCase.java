package com.takarub.esim.catalog.application.usecase;

import java.util.List;
import java.util.Locale;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.query.BrowseCatalogQuery;
import com.takarub.esim.catalog.application.result.CatalogPackageView;

/**
 * Read-only use case: lists available catalog packages for the storefront.
 */
public class BrowseCatalogUseCase {

    private final CatalogBrowsePort catalogBrowsePort;

    public BrowseCatalogUseCase(CatalogBrowsePort catalogBrowsePort) {
        this.catalogBrowsePort = catalogBrowsePort;
    }

    public List<CatalogPackageView> execute(BrowseCatalogQuery query) {
        String countryIso = normalizeCountryIso(query.countryIso());
        return catalogBrowsePort.findAvailablePackages(countryIso);
    }

    private static String normalizeCountryIso(String countryIso) {
        if (countryIso == null || countryIso.isBlank()) {
            return null;
        }
        return countryIso.trim().toUpperCase(Locale.ROOT);
    }
}
