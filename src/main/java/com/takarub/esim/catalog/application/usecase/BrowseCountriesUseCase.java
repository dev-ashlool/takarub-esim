package com.takarub.esim.catalog.application.usecase;

import java.util.List;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.result.CountryView;

/**
 * Read-only use case: lists countries that have at least one available catalog package.
 */
public class BrowseCountriesUseCase {

    private final CatalogBrowsePort catalogBrowsePort;

    public BrowseCountriesUseCase(CatalogBrowsePort catalogBrowsePort) {
        this.catalogBrowsePort = catalogBrowsePort;
    }

    public List<CountryView> execute() {
        return catalogBrowsePort.findCountriesWithAvailablePackages();
    }
}
