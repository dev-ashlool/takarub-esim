package com.takarub.esim.commerce.domain.order;

import java.math.BigDecimal;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Immutable commercial line snapshot supplied by the application when creating an {@link Order}.
 * Mirrors the cart commercial package fields plus quantity and the frozen supplier product
 * selection resolved at checkout. The order domain does not load Catalog, Cart, or Supplier.
 */
public record OrderItemSnapshot(
        String packageId,
        String countryIso,
        String countryNameArabic,
        String countryNameEnglish,
        LocationType locationType,
        int dataAmount,
        DataUnit dataUnit,
        int durationDays,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        String supplierKey,
        String remoteProductId,
        BigDecimal supplierCostAtCheckout,
        String supplierCostCurrency) {
}
