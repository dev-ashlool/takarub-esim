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
import com.takarub.esim.catalog.domain.model.NormalizedCost;
import com.takarub.esim.catalog.domain.service.CurrencyNormalizationService;
import com.takarub.esim.catalog.infrastructure.cache.CatalogCacheInvalidator;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.supplier.application.command.SyncSupplierCatalogCommand;
import com.takarub.esim.supplier.application.port.SupplierCredentialsPort;
import com.takarub.esim.supplier.application.port.SupplierLikeCardProductLogPort;
import com.takarub.esim.supplier.application.port.SupplierPackageMappingPort;
import com.takarub.esim.supplier.application.port.SupplierSyncAuditLogPort;
import com.takarub.esim.supplier.application.port.SyncChangeLogPort;
import com.takarub.esim.supplier.application.result.MappingUpsertResult;
import com.takarub.esim.supplier.application.result.MarkOutOfStockResult;
import com.takarub.esim.supplier.application.result.SyncSupplierCatalogResult;
import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;
import com.takarub.esim.supplier.domain.model.CountryInfo;
import com.takarub.esim.supplier.domain.model.LocationClassifier;
import com.takarub.esim.supplier.domain.model.LocationType;
import com.takarub.esim.supplier.domain.model.RawSupplierProduct;
import com.takarub.esim.supplier.domain.model.RegionNormalizer;
import com.takarub.esim.supplier.domain.model.SupplierSyncAuditLog;
import com.takarub.esim.supplier.domain.model.SupplierType;
import com.takarub.esim.supplier.domain.model.SyncChangeLog;
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
    private final SyncChangeLogPort syncChangeLogPort;
    private final CatalogCacheInvalidator cacheInvalidator;
    private final CurrencyNormalizationService currencyNormalizationService;

    public SyncSupplierCatalogUseCase(TransactionRunner transactionRunner,
                                      SupplierCredentialsPort credentialsPort,
                                      LikeCardSupplierAdapter likeCardSupplierAdapter,
                                      SupplierLikeCardProductLogPort likeCardProductLogPort,
                                      CatalogPackagePort catalogPackagePort,
                                      SupplierPackageMappingPort packageMappingPort,
                                      SupplierSyncAuditLogPort auditLogPort,
                                      SyncChangeLogPort syncChangeLogPort,
                                      CatalogCacheInvalidator cacheInvalidator,
                                      CurrencyNormalizationService currencyNormalizationService) {
        this.transactionRunner = transactionRunner;
        this.credentialsPort = credentialsPort;
        this.likeCardSupplierAdapter = likeCardSupplierAdapter;
        this.likeCardProductLogPort = likeCardProductLogPort;
        this.catalogPackagePort = catalogPackagePort;
        this.packageMappingPort = packageMappingPort;
        this.auditLogPort = auditLogPort;
        this.syncChangeLogPort = syncChangeLogPort;
        this.cacheInvalidator = cacheInvalidator;
        this.currencyNormalizationService = currencyNormalizationService;
    }

    public SyncSupplierCatalogResult execute(SyncSupplierCatalogCommand command) {
        String supplierKey = command.supplierKey().toUpperCase();
        SupplierSyncAuditLog auditLog = new SupplierSyncAuditLog(supplierKey);
        auditLog = auditLogPort.save(auditLog);

        try {
            SyncSupplierCatalogResult result = synchronize(supplierKey, auditLog.getId());
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

    private SyncSupplierCatalogResult synchronize(String normalizedSupplierKey, Long syncAuditLogId) {
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
        List<SyncChangeLog> changeLogs = new ArrayList<>();

        for (RawSupplierProduct product : products) {
            try {
                likeCardProductLogPort.saveOrUpdate(product, syncedAt);

                String catalogPackageId = catalogPackagePort.resolvePackageId(
                        product.countryIso(),
                        product.dataAmount(),
                        product.dataUnit(),
                        product.durationDays());

                NormalizedCost normalizedCost = currencyNormalizationService.normalize(
                        product.costPrice(), product.costCurrency());

                MappingUpsertResult upsert = packageMappingPort.upsertInStock(
                        catalogPackageId,
                        normalizedSupplierKey,
                        product.id(),
                        normalizedCost.originalAmount(),
                        normalizedCost.originalCurrency(),
                        normalizedCost.normalizedAmount(),
                        normalizedCost.normalizedCurrency());

                if (syncAuditLogId != null && upsert.shouldLogChange()) {
                    changeLogs.add(toCostChangeLog(syncAuditLogId, normalizedSupplierKey, upsert, syncedAt));
                }

                presentRemoteIds.add(product.id());
                availableCatalogPackageIds.add(catalogPackageId);
                mappingsUpserted++;
            } catch (Exception ex) {
                log.warn("Failed to persist product id={} country={}: {}",
                        product.id(), product.countryIso(), ex.getMessage());
            }
        }

        MarkOutOfStockResult outOfStockResult =
                packageMappingPort.markOutOfStockExcept(normalizedSupplierKey, presentRemoteIds);
        if (syncAuditLogId != null) {
            for (MarkOutOfStockResult.OutOfStockMapping mapping : outOfStockResult.markedOutOfStock()) {
                changeLogs.add(SyncChangeLog.stockOut(
                        syncAuditLogId,
                        normalizedSupplierKey,
                        mapping.remoteProductId(),
                        mapping.catalogPackageId(),
                        mapping.costPrice(),
                        mapping.costCurrency(),
                        mapping.normalizedCost(),
                        mapping.normalizedCurrency(),
                        syncedAt));
            }
        }

        if (!changeLogs.isEmpty()) {
            syncChangeLogPort.saveAll(changeLogs);
        }

        catalogPackagePort.markAvailable(availableCatalogPackageIds);

        int catalogPackagesMarkedUnavailable =
                catalogPackagePort.markUnavailableExcept(availableCatalogPackageIds);

        return new SyncSupplierCatalogResult(
                normalizedSupplierKey,
                products.size(),
                mappingsUpserted,
                outOfStockResult.count(),
                catalogPackagesMarkedUnavailable,
                harvest.regionsProcessed,
                harvest.invalidLocations);
    }

    private static SyncChangeLog toCostChangeLog(Long syncAuditLogId, String supplier,
                                                 MappingUpsertResult upsert, Instant changedAt) {
        if (upsert.created()) {
            return SyncChangeLog.created(
                    syncAuditLogId,
                    supplier,
                    upsert.remoteProductId(),
                    upsert.catalogPackageId(),
                    upsert.newCostPrice(),
                    upsert.newCostCurrency(),
                    upsert.newNormalizedCost(),
                    upsert.newNormalizedCurrency(),
                    changedAt);
        }
        return SyncChangeLog.costUpdated(
                syncAuditLogId,
                supplier,
                upsert.remoteProductId(),
                upsert.catalogPackageId(),
                upsert.oldCostPrice(),
                upsert.newCostPrice(),
                upsert.oldCostCurrency(),
                upsert.newCostCurrency(),
                upsert.oldNormalizedCost(),
                upsert.newNormalizedCost(),
                upsert.oldNormalizedCurrency(),
                upsert.newNormalizedCurrency(),
                changedAt);
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
                    log.debug("Category {} location {} ({}) returned {} products",
                            categoryId, country.iso(), locationType, products.size());
                } catch (SupplierApiException ex) {
                    log.warn("Failed to fetch products for category {} location {}: {}",
                            categoryId, country.iso(), ex.getMessage());
                }
            }
        }

        log.info("Harvest complete: {} unique products, {} regions, {} invalid locations",
                deduplicated.size(), regionsProcessed, invalidLocations);

        return new HarvestResult(new ArrayList<>(deduplicated.values()), regionsProcessed, invalidLocations);
    }

    private record HarvestResult(List<RawSupplierProduct> products, int regionsProcessed, int invalidLocations) {}
}
