package com.takarub.esim.commerce.domain.order;

import java.math.BigDecimal;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Immutable commercial line snapshot supplied by the application when creating an {@link Order}.
 * Mirrors the cart commercial package fields plus quantity; the order domain does not load Catalog
 * or Cart aggregates.
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
        int quantity) {
}
