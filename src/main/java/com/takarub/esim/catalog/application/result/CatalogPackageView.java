package com.takarub.esim.catalog.application.result;

import com.takarub.esim.supplier.domain.model.DataUnit;

/**
 * Read model for a client-facing catalog package.
 */
public record CatalogPackageView(
        String id,
        String countryIso,
        String countryArabicName,
        String countryEnglishName,
        String flagImageUrl,
        int dataAmount,
        DataUnit dataUnit,
        int durationDays) {
}
