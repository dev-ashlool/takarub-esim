package com.takarub.esim.supplier.application.port;

import java.math.BigDecimal;
import java.util.Set;

import com.takarub.esim.supplier.application.result.MappingUpsertResult;
import com.takarub.esim.supplier.application.result.MarkOutOfStockResult;

/**
 * Manages supplier-to-catalog package mappings and presence-based stock flags.
 */
public interface SupplierPackageMappingPort {

    /**
     * Upserts an in-stock mapping.
     * {@code costPrice}/{@code costCurrency} are ORIGINAL supplier values.
     * {@code normalizedCostPrice}/{@code normalizedCurrency} are USD-comparable values.
     *
     * @return whether the row was created and whether cost fields changed
     */
    MappingUpsertResult upsertInStock(String catalogPackageId, String supplierKey, String remoteProductId,
                                      BigDecimal costPrice, String costCurrency,
                                      BigDecimal normalizedCostPrice, String normalizedCurrency);

    /**
     * Marks mappings for {@code supplierKey} out of stock when their remote id is absent.
     *
     * @return details of mappings that transitioned from in-stock to out-of-stock
     */
    MarkOutOfStockResult markOutOfStockExcept(String supplierKey, Set<String> presentRemoteProductIds);

    /**
     * Recalculates {@code normalized_*} from original {@code cost_*} for mappings whose
     * original currency matches {@code originalCurrency}. Does not modify original cost fields.
     * Caller must not invoke this for USD originals (no-op expected).
     *
     * @return number of mappings updated
     */
    int recalculateNormalizedCosts(String originalCurrency, BigDecimal rateToUsd);
}
