package com.takarub.esim.supplier.domain.port;

import java.util.List;
import java.util.Map;

import com.takarub.esim.supplier.domain.model.RawSupplierProduct;
import com.takarub.esim.supplier.domain.model.SupplierType;

/**
 * Outbound port for harvesting remote supplier catalog inventory.
 */
public interface SupplierCatalogClient {

    List<RawSupplierProduct> fetchRemoteCatalog(Map<String, String> credentials);

    SupplierType getSupplierType();
}
