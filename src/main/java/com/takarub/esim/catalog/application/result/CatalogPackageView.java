package com.takarub.esim.catalog.application.result;

import java.math.BigDecimal;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Read model for a client-facing catalog package including sell price.
 */
public record CatalogPackageView(
        String id,
        String countryIso,
        String countryArabicName,
        String countryEnglishName,
        String flagImageUrl,
        int dataAmount,
        DataUnit dataUnit,
        int durationDays,
        LocationType locationType,
        BigDecimal price,
        String priceCurrency,
        String countrySlug) {
}
