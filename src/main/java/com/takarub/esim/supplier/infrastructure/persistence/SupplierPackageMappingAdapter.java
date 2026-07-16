package com.takarub.esim.supplier.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.takarub.esim.catalog.infrastructure.persistence.CatalogPackageEntity;
import com.takarub.esim.supplier.application.port.SupplierPackageMappingPort;

/**
 * JPA-backed supplier package mapping upserts and presence-based stock toggles.
 */
@Component
public class SupplierPackageMappingAdapter implements SupplierPackageMappingPort {

    private final SupplierPackageMappingJpaRepository repository;
    private final CatalogPackageEntityResolver catalogPackageEntityResolver;

    public SupplierPackageMappingAdapter(SupplierPackageMappingJpaRepository repository,
                                         CatalogPackageEntityResolver catalogPackageEntityResolver) {
        this.repository = repository;
        this.catalogPackageEntityResolver = catalogPackageEntityResolver;
    }

    @Override
    public void upsertInStock(String catalogPackageId, String supplierKey, String remoteProductId,
                              BigDecimal costPrice, String costCurrency) {
        CatalogPackageEntity catalogPackage = catalogPackageEntityResolver.requireById(catalogPackageId);
        SupplierPackageMappingEntity mapping = repository
                .findBySupplierKeyAndRemoteProductId(supplierKey, remoteProductId)
                .orElseGet(() -> new SupplierPackageMappingEntity(
                        catalogPackage, supplierKey, remoteProductId, costPrice, costCurrency, true));

        mapping.setCostPrice(costPrice);
        mapping.setCostCurrency(costCurrency);
        mapping.setInStock(true);
        repository.save(mapping);
    }

    @Override
    public int markOutOfStockExcept(String supplierKey, Set<String> presentRemoteProductIds) {
        int marked = 0;
        for (SupplierPackageMappingEntity mapping : repository.findAllBySupplierKey(supplierKey)) {
            if (!presentRemoteProductIds.contains(mapping.getRemoteProductId()) && mapping.isInStock()) {
                mapping.setInStock(false);
                repository.save(mapping);
                marked++;
            }
        }
        return marked;
    }
}
