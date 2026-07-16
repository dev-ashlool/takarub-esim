package com.takarub.esim.supplier.application.usecases;



import java.time.Instant;

import java.util.ArrayList;

import java.util.HashSet;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.Set;



import com.takarub.esim.catalog.application.port.CatalogPackagePort;

import com.takarub.esim.identity.application.port.TransactionRunner;

import com.takarub.esim.supplier.application.command.SyncSupplierCatalogCommand;

import com.takarub.esim.supplier.application.port.SupplierCredentialsPort;

import com.takarub.esim.supplier.application.port.SupplierLikeCardProductLogPort;

import com.takarub.esim.supplier.application.port.SupplierPackageMappingPort;

import com.takarub.esim.supplier.application.result.SyncSupplierCatalogResult;

import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;

import com.takarub.esim.supplier.domain.model.RawSupplierProduct;

import com.takarub.esim.supplier.domain.model.SupplierType;

import com.takarub.esim.supplier.infrastructure.adapters.likecard.LikeCardSupplierAdapter;



/**

 * Synchronizes a supplier remote catalog into local raw logs, catalog packages, and supplier mappings.

 */

public class SyncSupplierCatalogUseCase {



    private final TransactionRunner transactionRunner;

    private final SupplierCredentialsPort credentialsPort;

    private final LikeCardSupplierAdapter likeCardSupplierAdapter;

    private final SupplierLikeCardProductLogPort likeCardProductLogPort;

    private final CatalogPackagePort catalogPackagePort;

    private final SupplierPackageMappingPort packageMappingPort;



    public SyncSupplierCatalogUseCase(TransactionRunner transactionRunner,

                                      SupplierCredentialsPort credentialsPort,

                                      LikeCardSupplierAdapter likeCardSupplierAdapter,

                                      SupplierLikeCardProductLogPort likeCardProductLogPort,

                                      CatalogPackagePort catalogPackagePort,

                                      SupplierPackageMappingPort packageMappingPort) {

        this.transactionRunner = transactionRunner;

        this.credentialsPort = credentialsPort;

        this.likeCardSupplierAdapter = likeCardSupplierAdapter;

        this.likeCardProductLogPort = likeCardProductLogPort;

        this.catalogPackagePort = catalogPackagePort;

        this.packageMappingPort = packageMappingPort;

    }



    public SyncSupplierCatalogResult execute(SyncSupplierCatalogCommand command) {

        return transactionRunner.execute(() -> synchronize(command.supplierKey()));

    }



    private SyncSupplierCatalogResult synchronize(String supplierKey) {

        String normalizedSupplierKey = supplierKey.toUpperCase();

        if (!SupplierType.LIKE_CARD.name().equals(normalizedSupplierKey)) {

            throw new IllegalArgumentException("Unsupported supplier key for catalog sync: " + supplierKey);

        }

        if (likeCardSupplierAdapter.getSupplierType() != SupplierType.LIKE_CARD) {

            throw new IllegalStateException("Configured catalog client does not match LIKE_CARD supplier");

        }



        Map<String, String> credentials = credentialsPort.getCredentials(normalizedSupplierKey);

        List<RawSupplierProduct> products = harvestProducts(credentials);



        Set<String> presentRemoteIds = new HashSet<>();

        Set<String> availableCatalogPackageIds = new HashSet<>();

        int mappingsUpserted = 0;

        Instant syncedAt = Instant.now();



        for (RawSupplierProduct product : products) {

            likeCardProductLogPort.saveOrUpdate(product, syncedAt);



            String catalogPackageId = catalogPackagePort.resolvePackageId(

                    product.countryIso(),

                    product.dataAmount(),

                    product.dataUnit(),

                    product.durationDays());



            packageMappingPort.upsertInStock(

                    catalogPackageId,

                    normalizedSupplierKey,

                    product.id(),

                    product.costPrice(),

                    product.costCurrency());



            presentRemoteIds.add(product.id());

            availableCatalogPackageIds.add(catalogPackageId);

            mappingsUpserted++;

        }



        int mappingsMarkedOutOfStock =

                packageMappingPort.markOutOfStockExcept(normalizedSupplierKey, presentRemoteIds);



        catalogPackagePort.markAvailable(availableCatalogPackageIds);

        int catalogPackagesMarkedUnavailable =

                catalogPackagePort.markUnavailableExcept(availableCatalogPackageIds);



        return new SyncSupplierCatalogResult(

                normalizedSupplierKey,

                products.size(),

                mappingsUpserted,

                mappingsMarkedOutOfStock,

                catalogPackagesMarkedUnavailable);

    }



    private List<RawSupplierProduct> harvestProducts(Map<String, String> credentials) {

        Map<String, RawSupplierProduct> deduplicated = new LinkedHashMap<>();



        for (String categoryId : likeCardSupplierAdapter.fetchCategoryIds(credentials)) {

            List<String> countryIsos;

            try {

                countryIsos = likeCardSupplierAdapter.fetchCountryIsos(credentials, categoryId);

            } catch (SupplierApiException ex) {

                continue;

            }



            for (String countryIso : countryIsos) {

                try {

                    for (RawSupplierProduct product :

                            likeCardSupplierAdapter.fetchProducts(credentials, categoryId, countryIso)) {

                        deduplicated.put(product.id(), product);

                    }

                } catch (SupplierApiException ex) {

                    // Partial failure: skip this country and continue harvesting others.

                }

            }

        }



        return new ArrayList<>(deduplicated.values());

    }

}


