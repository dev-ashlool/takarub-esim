package com.takarub.esim.supplier.application.usecases;



import java.time.Instant;

import java.util.ArrayList;

import java.util.HashSet;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.Set;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import com.takarub.esim.catalog.application.port.CatalogPackagePort;

import com.takarub.esim.identity.application.port.TransactionRunner;

import com.takarub.esim.supplier.application.command.SyncSupplierCatalogCommand;

import com.takarub.esim.supplier.application.port.SupplierCredentialsPort;

import com.takarub.esim.supplier.application.port.SupplierLikeCardProductLogPort;

import com.takarub.esim.supplier.application.port.SupplierPackageMappingPort;

import com.takarub.esim.supplier.application.port.SupplierSyncAuditLogPort;

import com.takarub.esim.supplier.domain.model.LocationClassifier;

import com.takarub.esim.supplier.domain.model.LocationType;

import com.takarub.esim.supplier.domain.model.RegionNormalizer;

import com.takarub.esim.supplier.domain.model.SupplierSyncAuditLog;

import com.takarub.esim.supplier.application.result.SyncSupplierCatalogResult;

import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;

import com.takarub.esim.supplier.domain.model.CountryInfo;

import com.takarub.esim.supplier.domain.model.RawSupplierProduct;

import com.takarub.esim.supplier.domain.model.SupplierType;

import com.takarub.esim.catalog.infrastructure.cache.CatalogCacheInvalidator;

import com.takarub.esim.supplier.infrastructure.adapters.likecard.LikeCardSupplierAdapter;



/**

 * Synchronizes a supplier remote catalog into local raw logs, catalog packages, and supplier mappings.

 */

public class SyncSupplierCatalogUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncSupplierCatalogUseCase.class);

    private final TransactionRunner transactionRunner;

    private final SupplierCredentialsPort credentialsPort;

    private final LikeCardSupplierAdapter likeCardSupplierAdapter;

    private final SupplierLikeCardProductLogPort likeCardProductLogPort;

    private final CatalogPackagePort catalogPackagePort;

    private final SupplierPackageMappingPort packageMappingPort;

    private final SupplierSyncAuditLogPort auditLogPort;

    private final CatalogCacheInvalidator cacheInvalidator;



    public SyncSupplierCatalogUseCase(TransactionRunner transactionRunner,

                                      SupplierCredentialsPort credentialsPort,

                                      LikeCardSupplierAdapter likeCardSupplierAdapter,

                                      SupplierLikeCardProductLogPort likeCardProductLogPort,

                                      CatalogPackagePort catalogPackagePort,

                                      SupplierPackageMappingPort packageMappingPort,

                                      SupplierSyncAuditLogPort auditLogPort,

                                      CatalogCacheInvalidator cacheInvalidator) {

        this.transactionRunner = transactionRunner;

        this.credentialsPort = credentialsPort;

        this.likeCardSupplierAdapter = likeCardSupplierAdapter;

        this.likeCardProductLogPort = likeCardProductLogPort;

        this.catalogPackagePort = catalogPackagePort;

        this.packageMappingPort = packageMappingPort;

        this.auditLogPort = auditLogPort;

        this.cacheInvalidator = cacheInvalidator;

    }



    public SyncSupplierCatalogResult execute(SyncSupplierCatalogCommand command) {

        String supplierKey = command.supplierKey().toUpperCase();
        SupplierSyncAuditLog auditLog = new SupplierSyncAuditLog(supplierKey);
        auditLog = auditLogPort.save(auditLog);

        try {
            SyncSupplierCatalogResult result = synchronize(supplierKey);
            int failedCount = result.productsFetched() - result.mappingsUpserted();
            auditLog.markSuccess(
                    result.productsFetched(),
                    result.mappingsUpserted(),
                    result.mappingsMarkedOutOfStock(),
                    failedCount,
                    0,
                    result.invalidLocationCount(),
                    result.regionsProcessedCount());
            auditLogPort.save(auditLog);
            cacheInvalidator.invalidateAll();
            return result;
        } catch (Exception ex) {
            auditLog.markFailed(ex.getMessage());
            auditLogPort.save(auditLog);
            throw ex;
        }

    }



    private SyncSupplierCatalogResult synchronize(String normalizedSupplierKey) {

        if (!SupplierType.LIKE_CARD.name().equals(normalizedSupplierKey)) {

            throw new IllegalArgumentException("Unsupported supplier key for catalog sync: " + normalizedSupplierKey);

        }

        if (likeCardSupplierAdapter.getSupplierType() != SupplierType.LIKE_CARD) {

            throw new IllegalStateException("Configured catalog client does not match LIKE_CARD supplier");

        }



        Map<String, String> credentials = credentialsPort.getCredentials(normalizedSupplierKey);

        HarvestResult harvest = harvestProducts(credentials);

        List<RawSupplierProduct> products = harvest.products;



        Set<String> presentRemoteIds = new HashSet<>();

        Set<String> availableCatalogPackageIds = new HashSet<>();

        int mappingsUpserted = 0;

        Instant syncedAt = Instant.now();



        for (RawSupplierProduct product : products) {
            try {
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
            } catch (Exception ex) {
                log.warn("Failed to persist product id={} country={}: {}",
                        product.id(), product.countryIso(), ex.getMessage());
            }
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

                catalogPackagesMarkedUnavailable,

                harvest.regionsProcessed,

                harvest.invalidLocations);

    }



    private HarvestResult harvestProducts(Map<String, String> credentials) {

        Map<String, RawSupplierProduct> deduplicated = new LinkedHashMap<>();
        int regionsProcessed = 0;
        int invalidLocations = 0;

        List<String> categoryIds = likeCardSupplierAdapter.fetchCategoryIds(credentials);

        log.info("Fetched {} category IDs from LikeCard", categoryIds.size());

        for (String categoryId : categoryIds) {

            List<CountryInfo> countries;

            try {

                countries = likeCardSupplierAdapter.fetchCountries(credentials, categoryId);

                log.info("Category {} returned {} countries", categoryId, countries.size());

            } catch (SupplierApiException ex) {

                log.warn("Failed to fetch countries for category {}: {}", categoryId, ex.getMessage());

                continue;

            }



            for (CountryInfo country : countries) {

                LocationClassifier.Classification classification = LocationClassifier.classify(country.iso());

                if (classification == LocationClassifier.Classification.INVALID) {
                    log.warn("Skipping invalid location value: {}", country.iso());
                    invalidLocations++;
                    continue;
                }

                LocationType locationType = classification == LocationClassifier.Classification.COUNTRY
                        ? LocationType.COUNTRY : LocationType.REGION;
                String displayName = locationType == LocationType.REGION
                        ? RegionNormalizer.normalize(country.iso()) : country.name();

                if (locationType == LocationType.REGION) {
                    regionsProcessed++;
                }

                try {
                    catalogPackagePort.ensureLocation(country.iso(), displayName, country.imageUrl(), locationType);
                } catch (Exception ex) {
                    log.warn("Failed to ensure location {} ({}): {}", country.iso(), locationType, ex.getMessage());
                }

                try {

                    List<RawSupplierProduct> products =

                            likeCardSupplierAdapter.fetchProducts(credentials, categoryId, country.iso());

                    for (RawSupplierProduct product : products) {

                        deduplicated.put(product.id(), product);

                    }

                    log.debug("Category {} location {} ({}) returned {} products", categoryId, country.iso(), locationType, products.size());

                } catch (SupplierApiException ex) {

                    log.warn("Failed to fetch products for category {} location {}: {}", categoryId, country.iso(), ex.getMessage());

                }

            }

        }

        log.info("Harvest complete: {} unique products, {} regions, {} invalid locations",
                deduplicated.size(), regionsProcessed, invalidLocations);

        return new HarvestResult(new ArrayList<>(deduplicated.values()), regionsProcessed, invalidLocations);

    }

    private record HarvestResult(List<RawSupplierProduct> products, int regionsProcessed, int invalidLocations) {}

}


