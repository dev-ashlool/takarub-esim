package com.takarub.esim.catalog.application.usecase;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.query.GetPackageDetailsQuery;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.catalog.domain.exceptions.PackageNotFoundException;

/**
 * Read-only use case: returns full details for a single catalog package.
 */
public class PackageDetailsUseCase {

    private final CatalogBrowsePort catalogBrowsePort;

    public PackageDetailsUseCase(CatalogBrowsePort catalogBrowsePort) {
        this.catalogBrowsePort = catalogBrowsePort;
    }

    public PackageDetailsView execute(GetPackageDetailsQuery query) {
        return catalogBrowsePort.findPackageById(query.packageId())
                .orElseThrow(() -> new PackageNotFoundException(
                        "Catalog package not found: " + query.packageId()));
    }
}
