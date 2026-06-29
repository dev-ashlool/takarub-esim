package com.takarub.esim.catalog.application.query;

/**
 * Optional country filter for catalog browsing.
 *
 * @param countryIso ISO alpha-2 country code; {@code null} or blank returns all available packages
 */
public record BrowseCatalogQuery(String countryIso) {
}
