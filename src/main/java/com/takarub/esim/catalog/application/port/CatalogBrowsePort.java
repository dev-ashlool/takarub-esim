package com.takarub.esim.catalog.application.port;

import java.util.List;

import com.takarub.esim.catalog.application.result.CatalogPackageView;

/**
 * Read-side port for browsing client-facing catalog packages.
 */
public interface CatalogBrowsePort {

    List<CatalogPackageView> findAvailablePackages(String countryIso);
}
