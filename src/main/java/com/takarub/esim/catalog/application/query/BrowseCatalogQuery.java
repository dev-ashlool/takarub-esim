package com.takarub.esim.catalog.application.query;

/**
 * Optional country filter for catalog browsing.
 *
 * @param countryIso legacy ISO / location id filter
 * @param countrySlug SEO slug filter; takes precedence over {@code countryIso} when both are set
 */
public record BrowseCatalogQuery(String countryIso, String countrySlug) {

    public BrowseCatalogQuery(String countryIso) {
        this(countryIso, null);
    }
}
