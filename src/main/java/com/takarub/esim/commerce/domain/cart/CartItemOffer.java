package com.takarub.esim.commerce.domain.cart;

import java.math.BigDecimal;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Validated commercial package snapshot assembled by the application layer and accepted by
 * {@link Cart}. The cart domain never loads Catalog or Pricing; it only validates and stores
 * this offer as seen by the customer.
 */
public record CartItemOffer(
        String packageId,
        String countryIso,
        String countryNameArabic,
        String countryNameEnglish,
        LocationType locationType,
        int dataAmount,
        DataUnit dataUnit,
        int durationDays,
        BigDecimal unitPrice,
        String currency) {
}
