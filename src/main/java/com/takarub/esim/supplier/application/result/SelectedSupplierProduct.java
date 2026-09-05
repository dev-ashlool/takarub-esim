package com.takarub.esim.supplier.application.result;

import java.math.BigDecimal;

/**
 * Winning in-stock supplier mapping selected for a catalog package at checkout time.
 * {@code supplierCostAtCheckout}/{@code supplierCostCurrency} are the ORIGINAL mapping cost
 * fields (not normalized USD-comparable values).
 */
public record SelectedSupplierProduct(
        String supplierKey,
        String remoteProductId,
        BigDecimal supplierCostAtCheckout,
        String supplierCostCurrency) {
}
