package com.takarub.esim.supplier.application.result;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of marking absent mappings out of stock, with enough detail to write change logs.
 */
public record MarkOutOfStockResult(List<OutOfStockMapping> markedOutOfStock) {

    public int count() {
        return markedOutOfStock.size();
    }

    public record OutOfStockMapping(
            String catalogPackageId,
            String remoteProductId,
            BigDecimal costPrice,
            String costCurrency,
            BigDecimal normalizedCost,
            String normalizedCurrency) {
    }
}
