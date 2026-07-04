package com.takarub.esim.supplier.application.result;

/**
 * Outcome summary of a supplier catalog synchronization run.
 */
public record SyncSupplierCatalogResult(
        String supplierKey,
        int productsFetched,
        int mappingsUpserted,
        int mappingsMarkedOutOfStock,
        int catalogPackagesMarkedUnavailable,
        int regionsProcessedCount,
        int invalidLocationCount) {
}
