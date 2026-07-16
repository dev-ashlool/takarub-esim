package com.takarub.esim.catalog.application.result;

import java.math.BigDecimal;

import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Read model for a catalog location (country or region) with available package count.
 */
public record CountryView(
        String iso,
        String arabicName,
        String englishName,
        String flagImageUrl,
        long packageCount,
        LocationType locationType,
        String slug,
        BigDecimal minimumPrice) {
}
