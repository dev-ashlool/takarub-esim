package com.takarub.esim.catalog.presentation.response;

import java.math.BigDecimal;

/**
 * REST response for a browsable catalog package.
 */
public record CatalogPackageResponse(
        String id,
        String countryIso,
        String countryArabicName,
        String countryEnglishName,
        String flagImageUrl,
        int dataAmount,
        String dataUnit,
        int durationDays,
        String locationType,
        BigDecimal price,
        String priceCurrency) {
}
