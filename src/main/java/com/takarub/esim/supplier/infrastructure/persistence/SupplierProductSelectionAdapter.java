package com.takarub.esim.supplier.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.supplier.application.port.SupplierProductSelectionPort;
import com.takarub.esim.supplier.application.result.SelectedSupplierProduct;

/**
 * JPA-backed deterministic supplier product selection for checkout.
 */
@Component
public class SupplierProductSelectionAdapter implements SupplierProductSelectionPort {

    private final SupplierPackageMappingJpaRepository repository;

    public SupplierProductSelectionAdapter(SupplierPackageMappingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SelectedSupplierProduct> findWinningInStockMapping(String catalogPackageId) {
        if (catalogPackageId == null || catalogPackageId.isBlank()) {
            return Optional.empty();
        }
        return repository
                .findFirstByCatalogPackage_IdAndInStockTrueOrderByNormalizedCostPriceAscSupplierKeyAscRemoteProductIdAsc(
                        catalogPackageId.trim())
                .map(this::toSelected);
    }

    private SelectedSupplierProduct toSelected(SupplierPackageMappingEntity mapping) {
        return new SelectedSupplierProduct(
                mapping.getSupplierKey(),
                mapping.getRemoteProductId(),
                mapping.getCostPrice(),
                mapping.getCostCurrency());
    }
}
