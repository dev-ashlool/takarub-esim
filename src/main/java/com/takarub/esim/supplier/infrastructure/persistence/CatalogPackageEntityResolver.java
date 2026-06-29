package com.takarub.esim.supplier.infrastructure.persistence;

import org.springframework.stereotype.Component;

import com.takarub.esim.catalog.domain.exceptions.PackageNotFoundException;
import com.takarub.esim.catalog.infrastructure.persistence.CatalogPackageEntity;
import com.takarub.esim.catalog.infrastructure.persistence.CatalogPackageJpaRepository;

/**
 * Resolves catalog package entities for supplier infrastructure adapters.
 */
@Component
public class CatalogPackageEntityResolver {

    private final CatalogPackageJpaRepository catalogPackageJpaRepository;

    public CatalogPackageEntityResolver(CatalogPackageJpaRepository catalogPackageJpaRepository) {
        this.catalogPackageJpaRepository = catalogPackageJpaRepository;
    }

    public CatalogPackageEntity requireById(String catalogPackageId) {
        return catalogPackageJpaRepository.findById(catalogPackageId)
                .orElseThrow(() -> new PackageNotFoundException(
                        "Catalog package not found: " + catalogPackageId));
    }
}
