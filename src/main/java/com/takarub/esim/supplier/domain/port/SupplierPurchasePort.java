package com.takarub.esim.supplier.domain.port;

/**
 * Outbound port for purchasing a supplier product for one fulfillment work item.
 *
 * <p>Credentials are resolved by the adapter implementation; they must not appear on the request.
 */
public interface SupplierPurchasePort {

    SupplierPurchaseResult purchase(SupplierPurchaseRequest request);
}
