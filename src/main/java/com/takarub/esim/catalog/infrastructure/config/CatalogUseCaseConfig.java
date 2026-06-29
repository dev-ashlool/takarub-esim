package com.takarub.esim.catalog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.usecase.BrowseCatalogUseCase;

/**
 * Explicit Spring wiring for catalog application use cases.
 */
@Configuration
public class CatalogUseCaseConfig {

    @Bean
    public BrowseCatalogUseCase browseCatalogUseCase(CatalogBrowsePort catalogBrowsePort) {
        return new BrowseCatalogUseCase(catalogBrowsePort);
    }
}
