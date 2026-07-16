package com.takarub.esim.catalog.presentation.response;

import java.math.BigDecimal;

public record CountryResponse(
        String iso,
        String arabicName,
        String englishName,
        String flagImageUrl,
        long packageCount,
        String locationType,
        String slug,
        BigDecimal minimumPrice) {
}
