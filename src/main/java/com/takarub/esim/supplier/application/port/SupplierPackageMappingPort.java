package com.takarub.esim.supplier.application.port;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Manages supplier-to-catalog package mappings and presence-based stock flags.
 */
public interface SupplierPackageMappingPort {

    void upsertInStock(String catalogPackageId, String supplierKey, String remoteProductId,
                       BigDecimal costPrice, String costCurrency);

    int markOutOfStockExcept(String supplierKey, Set<String> presentRemoteProductIds);
}
