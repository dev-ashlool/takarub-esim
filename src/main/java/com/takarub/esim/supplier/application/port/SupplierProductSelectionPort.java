package com.takarub.esim.supplier.application.port;

import java.util.Optional;

import com.takarub.esim.supplier.application.result.SelectedSupplierProduct;

/**
 * Read-side port that selects exactly one in-stock supplier mapping for a catalog package.
 */
public interface SupplierProductSelectionPort {

    /**
     * Returns the winning in-stock mapping for {@code catalogPackageId}, or empty when none exist.
     * Winner order: normalized cost ascending, then supplier key, then remote product id.
     */
    Optional<SelectedSupplierProduct> findWinningInStockMapping(String catalogPackageId);
}
