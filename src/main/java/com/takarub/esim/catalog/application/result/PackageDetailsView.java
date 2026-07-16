package com.takarub.esim.catalog.application.result;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Read model for detailed information about a single catalog package.
 */
public record PackageDetailsView(
        String id,
        String countryIso,
        String countryArabicName,
        String countryEnglishName,
        String flagImageUrl,
        int dataAmount,
        DataUnit dataUnit,
        int durationDays,
        boolean available,
        LocationType locationType) {
}
