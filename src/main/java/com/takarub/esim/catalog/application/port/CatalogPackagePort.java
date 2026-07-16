package com.takarub.esim.catalog.application.port;

import java.util.Set;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Catalog package lookup and availability management driven by technical fingerprints.
 */
public interface CatalogPackagePort {

    /**
     * Creates or updates a country/region record with display name and flag image URL.
     */
    void ensureCountry(String iso, String englishName, String flagImageUrl);

    /**
     * Creates or updates a location (country or region) record with type classification.
     */
    void ensureLocation(String locationId, String displayName, String flagImageUrl, LocationType locationType);

    String resolvePackageId(String countryIso, int dataAmount, DataUnit dataUnit, int durationDays);

    void markAvailable(Set<String> catalogPackageIds);

    int markUnavailableExcept(Set<String> availableCatalogPackageIds);
}
