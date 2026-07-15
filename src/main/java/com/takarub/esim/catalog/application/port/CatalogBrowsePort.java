package com.takarub.esim.catalog.application.port;

import java.util.List;
import java.util.Optional;

import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.catalog.application.result.CountryView;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.catalog.application.result.PagedResult;

/**
 * Read-side port for browsing client-facing catalog packages.
 */
public interface CatalogBrowsePort {

    List<CatalogPackageView> findAvailablePackages(String countryIso);

    List<CountryView> findCountriesWithAvailablePackages();

    Optional<PackageDetailsView> findPackageById(String packageId);

    Optional<String> findCountryIdBySlug(String slug);

    PagedResult<CatalogPackageView> searchAvailablePackages(
            String searchTerm, String countryIso, Integer dataAmount,
            String dataUnit, Integer durationDays, int page, int size);
}
