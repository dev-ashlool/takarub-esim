package com.takarub.esim.catalog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.usecase.BrowseCatalogUseCase;
import com.takarub.esim.catalog.application.usecase.BrowseCountriesUseCase;
import com.takarub.esim.catalog.application.usecase.PackageDetailsUseCase;
import com.takarub.esim.catalog.application.usecase.SearchPackagesUseCase;

/**
 * Explicit Spring wiring for catalog application use cases.
 */
@Configuration
public class CatalogUseCaseConfig {

    @Bean
    public BrowseCatalogUseCase browseCatalogUseCase(CatalogBrowsePort catalogBrowsePort) {
        return new BrowseCatalogUseCase(catalogBrowsePort);
    }

    @Bean
    public BrowseCountriesUseCase browseCountriesUseCase(CatalogBrowsePort catalogBrowsePort) {
        return new BrowseCountriesUseCase(catalogBrowsePort);
    }

    @Bean
    public PackageDetailsUseCase packageDetailsUseCase(CatalogBrowsePort catalogBrowsePort) {
        return new PackageDetailsUseCase(catalogBrowsePort);
    }

    @Bean
    public SearchPackagesUseCase searchPackagesUseCase(CatalogBrowsePort catalogBrowsePort) {
        return new SearchPackagesUseCase(catalogBrowsePort);
    }
}
