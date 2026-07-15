package com.takarub.esim.catalog.presentation.response;

import java.math.BigDecimal;

/**
 * REST response for a single catalog package's full details.
 */
public record PackageDetailsResponse(
        String id,
        String countryIso,
        String countryArabicName,
        String countryEnglishName,
        String flagImageUrl,
        int dataAmount,
        String dataUnit,
        int durationDays,
        boolean available,
        String locationType,
        BigDecimal price,
        String priceCurrency,
        String slug) {
}
