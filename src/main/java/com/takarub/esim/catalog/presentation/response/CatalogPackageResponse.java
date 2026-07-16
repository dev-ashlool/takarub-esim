package com.takarub.esim.catalog.presentation.response;

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
        int durationDays) {
}
