package com.takarub.esim.supplier.domain.model;

/**
 * Lightweight carrier for country metadata harvested from a supplier API.
 */
public record CountryInfo(
        String iso,
        String name,
        String imageUrl) {

    public CountryInfo {
        if (iso == null || iso.isBlank()) {
            throw new IllegalArgumentException("iso must not be blank");
        }
    }
}
