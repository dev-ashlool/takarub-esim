package com.takarub.esim.catalog.application.port;

import java.util.Set;

import com.takarub.esim.supplier.domain.model.DataUnit;

/**
 * Catalog package lookup and availability management driven by technical fingerprints.
 */
public interface CatalogPackagePort {

    String resolvePackageId(String countryIso, int dataAmount, DataUnit dataUnit, int durationDays);

    void markAvailable(Set<String> catalogPackageIds);

    int markUnavailableExcept(Set<String> availableCatalogPackageIds);
}
