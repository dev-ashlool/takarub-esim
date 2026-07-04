package com.takarub.esim.catalog.application.query;

/**
 * Query to search catalog packages by free-text term and optional filters with pagination.
 */
public record SearchPackagesQuery(
        String searchTerm,
        String countryIso,
        Integer dataAmount,
        String dataUnit,
        Integer durationDays,
        int page,
        int size) {
}
