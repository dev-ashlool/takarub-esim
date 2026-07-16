package com.takarub.esim.catalog.presentation.response;

public record CountryResponse(
        String iso,
        String arabicName,
        String englishName,
        String flagImageUrl,
        long packageCount,
        String locationType) {
}
