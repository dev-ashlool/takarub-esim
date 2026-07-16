package com.takarub.esim.supplier.application.result;

import java.math.BigDecimal;

/**
 * Outcome of upserting a supplier package mapping, including previous cost values when updated.
 */
public record MappingUpsertResult(
        boolean created,
        boolean costChanged,
        String catalogPackageId,
        String remoteProductId,
        BigDecimal oldCostPrice,
        BigDecimal newCostPrice,
        String oldCostCurrency,
        String newCostCurrency,
        BigDecimal oldNormalizedCost,
        BigDecimal newNormalizedCost,
        String oldNormalizedCurrency,
        String newNormalizedCurrency) {

    public boolean shouldLogChange() {
        return created || costChanged;
    }
}
