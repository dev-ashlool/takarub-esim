package com.takarub.esim.supplier.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.application.port.CatalogPackagePort;
import com.takarub.esim.catalog.domain.exceptions.CurrencyExchangeException;
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
import com.takarub.esim.supplier.application.result.MarkOutOfStockResult.OutOfStockMapping;
import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;
import com.takarub.esim.supplier.domain.model.CountryInfo;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.RawSupplierProduct;
import com.takarub.esim.supplier.domain.model.SupplierSyncAuditLog;
import com.takarub.esim.supplier.domain.model.SupplierSyncAuditStatus;
import com.takarub.esim.supplier.domain.model.SupplierType;
import com.takarub.esim.supplier.domain.model.SyncChangeLog;
import com.takarub.esim.supplier.domain.model.SyncChangeType;
import com.takarub.esim.supplier.infrastructure.adapters.likecard.LikeCardSupplierAdapter;

@ExtendWith(MockitoExtension.class)
class SyncSupplierCatalogUseCaseTest {

    @Mock
    private TransactionRunner transactionRunner;
    @Mock
    private SupplierCredentialsPort credentialsPort;
    @Mock
    private LikeCardSupplierAdapter likeCardSupplierAdapter;
    @Mock
    private SupplierLikeCardProductLogPort likeCardProductLogPort;
    @Mock
    private CatalogPackagePort catalogPackagePort;
    @Mock
    private SupplierPackageMappingPort packageMappingPort;
    @Mock
    private SupplierSyncAuditLogPort auditLogPort;
    @Mock
    private SyncChangeLogPort syncChangeLogPort;
    @Mock
    private CatalogCacheInvalidator cacheInvalidator;
    @Mock
    private CurrencyNormalizationService currencyNormalizationService;

    private SyncSupplierCatalogUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SyncSupplierCatalogUseCase(
                transactionRunner,
                credentialsPort,
                likeCardSupplierAdapter,
                likeCardProductLogPort,
                catalogPackagePort,
                packageMappingPort,
                auditLogPort,
                syncChangeLogPort,
                cacheInvalidator,
                currencyNormalizationService);
        when(auditLogPort.save(any())).thenAnswer(invocation -> {
            SupplierSyncAuditLog log = invocation.getArgument(0);
            if (log.getId() != null) {
                return log;
            }
            return new SupplierSyncAuditLog(
                    1L,
                    log.getSupplier(),
                    log.getStatus(),
                    log.getStartedAt(),
                    log.getFinishedAt(),
                    log.getDurationMs(),
                    log.getTotalProcessed(),
                    log.getCreatedCount(),
                    log.getUpdatedCount(),
                    log.getFailedCount(),
                    log.getErrorMessage(),
                    log.getSkippedRegionsCount(),
                    log.getInvalidLocationCount(),
                    log.getRegionsProcessedCount());
        });
        lenient().when(likeCardSupplierAdapter.getSupplierType()).thenReturn(SupplierType.LIKE_CARD);
        lenient().when(credentialsPort.getCredentials("LIKE_CARD"))
                .thenReturn(Map.of("base_url", "https://api.example"));
        lenient().when(packageMappingPort.upsertInStock(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> createdUpsert(
                        invocation.getArgument(0),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4),
                        invocation.getArgument(5),
                        invocation.getArgument(6)));
        lenient().when(packageMappingPort.markOutOfStockExcept(any(), any()))
                .thenReturn(new MarkOutOfStockResult(List.of()));
    }

    @Test
    void synchronizesPresentProductsAndAppliesPresenceRules() {
        RawSupplierProduct product = new RawSupplierProduct(
                "5653", "JO", new BigDecimal("9.99"), "USD", 20, DataUnit.GB, 30);
        stubHarvest(List.of(product));
        stubUsdNormalization(new BigDecimal("9.99"));

        when(catalogPackagePort.resolvePackageId("JO", 20, DataUnit.GB, 30)).thenReturn("pkg-uuid-1");
        when(packageMappingPort.markOutOfStockExcept(eq("LIKE_CARD"), any())).thenReturn(
                new MarkOutOfStockResult(List.of(
                        new OutOfStockMapping("pkg-old-1", "111", new BigDecimal("1.00"), "USD",
                                new BigDecimal("1.00"), "USD"),
                        new OutOfStockMapping("pkg-old-2", "222", new BigDecimal("2.00"), "USD",
                                new BigDecimal("2.00"), "USD"))));
        when(catalogPackagePort.markUnavailableExcept(Set.of("pkg-uuid-1"))).thenReturn(1);

        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        assertThat(result.supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(result.productsFetched()).isEqualTo(1);
        assertThat(result.mappingsUpserted()).isEqualTo(1);
        assertThat(result.mappingsMarkedOutOfStock()).isEqualTo(2);
        assertThat(result.catalogPackagesMarkedUnavailable()).isEqualTo(1);

        verify(likeCardProductLogPort).saveOrUpdate(eq(product), any(Instant.class));
        verify(packageMappingPort).upsertInStock(
                "pkg-uuid-1", "LIKE_CARD", "5653",
                new BigDecimal("9.99"), "USD",
                new BigDecimal("9.99"), "USD");
        verify(catalogPackagePort).markAvailable(Set.of("pkg-uuid-1"));

        ArgumentCaptor<Set<String>> presentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(packageMappingPort).markOutOfStockExcept(eq("LIKE_CARD"), presentCaptor.capture());
        assertThat(presentCaptor.getValue()).containsExactly("5653");
    }

    @Test
    void persistsCreatedAndCostUpdatedAndStockOutChangeLogs() {
        RawSupplierProduct product = new RawSupplierProduct(
                "5653", "JO", new BigDecimal("10.50"), "USD", 20, DataUnit.GB, 30);
        stubHarvest(List.of(product));
        stubUsdNormalization(new BigDecimal("10.50"));
        when(catalogPackagePort.resolvePackageId("JO", 20, DataUnit.GB, 30)).thenReturn("pkg-uuid-1");
        when(packageMappingPort.upsertInStock(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new MappingUpsertResult(
                        false, true, "pkg-uuid-1", "5653",
                        new BigDecimal("9.99"), new BigDecimal("10.50"),
                        "USD", "USD",
                        new BigDecimal("9.99"), new BigDecimal("10.50"),
                        "USD", "USD"));
        when(packageMappingPort.markOutOfStockExcept(eq("LIKE_CARD"), any())).thenReturn(
                new MarkOutOfStockResult(List.of(
                        new OutOfStockMapping("pkg-gone", "999", new BigDecimal("3.00"), "USD",
                                new BigDecimal("3.00"), "USD"))));
        when(catalogPackagePort.markUnavailableExcept(any())).thenReturn(0);

        useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        ArgumentCaptor<List<SyncChangeLog>> changesCaptor = ArgumentCaptor.forClass(List.class);
        verify(syncChangeLogPort).saveAll(changesCaptor.capture());
        List<SyncChangeLog> changes = changesCaptor.getValue();
        assertThat(changes).hasSize(2);
        assertThat(changes.get(0).getChangeType()).isEqualTo(SyncChangeType.COST_UPDATED);
        assertThat(changes.get(0).getSyncAuditLogId()).isEqualTo(1L);
        assertThat(changes.get(0).getOldCostPrice()).isEqualByComparingTo("9.99");
        assertThat(changes.get(0).getNewCostPrice()).isEqualByComparingTo("10.50");
        assertThat(changes.get(1).getChangeType()).isEqualTo(SyncChangeType.STOCK_OUT);
        assertThat(changes.get(1).getRemoteProductId()).isEqualTo("999");
    }

    @Test
    void doesNotPersistChangeLogsWhenNothingChanged() {
        RawSupplierProduct product = new RawSupplierProduct(
                "5653", "JO", new BigDecimal("9.99"), "USD", 20, DataUnit.GB, 30);
        stubHarvest(List.of(product));
        stubUsdNormalization(new BigDecimal("9.99"));
        when(catalogPackagePort.resolvePackageId("JO", 20, DataUnit.GB, 30)).thenReturn("pkg-uuid-1");
        when(packageMappingPort.upsertInStock(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new MappingUpsertResult(
                        false, false, "pkg-uuid-1", "5653",
                        new BigDecimal("9.99"), new BigDecimal("9.99"),
                        "USD", "USD",
                        new BigDecimal("9.99"), new BigDecimal("9.99"),
                        "USD", "USD"));
        when(catalogPackagePort.markUnavailableExcept(any())).thenReturn(0);

        useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        verify(syncChangeLogPort, never()).saveAll(any());
    }

    @Test
    void persistsNormalizedCostForNonUsdSupplierCurrency() {
        RawSupplierProduct product = new RawSupplierProduct(
                "9001", "SA", new BigDecimal("37.50"), "SAR", 10, DataUnit.GB, 15);
        stubHarvest(List.of(product));

        when(currencyNormalizationService.normalize(new BigDecimal("37.50"), "SAR"))
                .thenReturn(new NormalizedCost(
                        new BigDecimal("37.50"), "SAR",
                        new BigDecimal("10.0000"), "USD"));
        when(catalogPackagePort.resolvePackageId("SA", 10, DataUnit.GB, 15)).thenReturn("pkg-sa");
        when(catalogPackagePort.markUnavailableExcept(any())).thenReturn(0);

        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        assertThat(result.mappingsUpserted()).isEqualTo(1);
        verify(packageMappingPort).upsertInStock(
                "pkg-sa", "LIKE_CARD", "9001",
                new BigDecimal("37.50"), "SAR",
                new BigDecimal("10.0000"), "USD");
    }

    @Test
    void skipsProductWhenNormalizationFailsDueToMissingRate() {
        RawSupplierProduct product = new RawSupplierProduct(
                "9002", "DE", new BigDecimal("20.00"), "EUR", 5, DataUnit.GB, 7);
        stubHarvest(List.of(product));

        when(currencyNormalizationService.normalize(new BigDecimal("20.00"), "EUR"))
                .thenThrow(new CurrencyExchangeException("No exchange rate found for EUR -> USD"));
        when(catalogPackagePort.resolvePackageId("DE", 5, DataUnit.GB, 7)).thenReturn("pkg-de");
        when(catalogPackagePort.markUnavailableExcept(Set.of())).thenReturn(0);

        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        assertThat(result.productsFetched()).isEqualTo(1);
        assertThat(result.mappingsUpserted()).isZero();
        verify(packageMappingPort, never()).upsertInStock(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void synchronizesProductsAcrossMultipleCategoriesAndCountries() {
        RawSupplierProduct jordanProduct = new RawSupplierProduct(
                "1001", "JO", new BigDecimal("9.99"), "USD", 5, DataUnit.GB, 7);
        RawSupplierProduct saudiProduct = new RawSupplierProduct(
                "2001", "SA", new BigDecimal("14.50"), "USD", 10, DataUnit.GB, 15);

        when(likeCardSupplierAdapter.fetchCategoryIds(any())).thenReturn(List.of("10", "20"));
        when(likeCardSupplierAdapter.fetchCountries(any(), eq("10"))).thenReturn(List.of(
                new CountryInfo("JO", "Jordan", null), new CountryInfo("AE", "UAE", null)));
        when(likeCardSupplierAdapter.fetchCountries(any(), eq("20"))).thenReturn(List.of(
                new CountryInfo("SA", "Saudi Arabia", null)));
        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("JO"))).thenReturn(List.of(jordanProduct));
        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("AE"))).thenReturn(List.of());
        when(likeCardSupplierAdapter.fetchProducts(any(), eq("20"), eq("SA"))).thenReturn(List.of(saudiProduct));

        when(currencyNormalizationService.normalize(any(), eq("USD"))).thenAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(0);
            return new NormalizedCost(amount, "USD", amount, "USD");
        });
        when(catalogPackagePort.resolvePackageId("JO", 5, DataUnit.GB, 7)).thenReturn("pkg-jo");
        when(catalogPackagePort.resolvePackageId("SA", 10, DataUnit.GB, 15)).thenReturn("pkg-sa");
        when(catalogPackagePort.markUnavailableExcept(any())).thenReturn(0);

        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        assertThat(result.productsFetched()).isEqualTo(2);
        assertThat(result.mappingsUpserted()).isEqualTo(2);
        verify(likeCardSupplierAdapter).fetchProducts(any(), eq("10"), eq("JO"));
        verify(likeCardSupplierAdapter).fetchProducts(any(), eq("20"), eq("SA"));
    }

    @Test
    void completesWithNoProductsWhenCategoriesAreEmpty() {
        when(likeCardSupplierAdapter.fetchCategoryIds(any())).thenReturn(List.of());
        when(packageMappingPort.markOutOfStockExcept(eq("LIKE_CARD"), eq(Set.of())))
                .thenReturn(new MarkOutOfStockResult(List.of()));
        when(catalogPackagePort.markUnavailableExcept(Set.of())).thenReturn(0);

        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        assertThat(result.productsFetched()).isZero();
        assertThat(result.mappingsUpserted()).isZero();
    }

    @Test
    void continuesSynchronizationWhenOneCountryProductsCallFails() {
        RawSupplierProduct saudiProduct = new RawSupplierProduct(
                "2001", "SA", new BigDecimal("14.50"), "USD", 10, DataUnit.GB, 15);

        when(likeCardSupplierAdapter.fetchCategoryIds(any())).thenReturn(List.of("10"));
        when(likeCardSupplierAdapter.fetchCountries(any(), eq("10"))).thenReturn(List.of(
                new CountryInfo("JO", "Jordan", null), new CountryInfo("SA", "Saudi Arabia", null)));
        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("JO")))
                .thenThrow(new SupplierApiException("LikeCard products API call failed"));
        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("SA"))).thenReturn(List.of(saudiProduct));

        stubUsdNormalization(new BigDecimal("14.50"));
        when(catalogPackagePort.resolvePackageId("SA", 10, DataUnit.GB, 15)).thenReturn("pkg-sa");
        when(catalogPackagePort.markUnavailableExcept(Set.of("pkg-sa"))).thenReturn(0);

        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        assertThat(result.productsFetched()).isEqualTo(1);
        verify(packageMappingPort).upsertInStock(
                "pkg-sa", "LIKE_CARD", "2001",
                new BigDecimal("14.50"), "USD",
                new BigDecimal("14.50"), "USD");
    }

    @Test
    void continuesSynchronizationWhenOneCategoryCountriesCallFails() {
        RawSupplierProduct saudiProduct = new RawSupplierProduct(
                "2001", "SA", new BigDecimal("14.50"), "USD", 10, DataUnit.GB, 15);

        when(likeCardSupplierAdapter.fetchCategoryIds(any())).thenReturn(List.of("10", "20"));
        when(likeCardSupplierAdapter.fetchCountries(any(), eq("10")))
                .thenThrow(new SupplierApiException("LikeCard countries API call failed"));
        when(likeCardSupplierAdapter.fetchCountries(any(), eq("20"))).thenReturn(List.of(
                new CountryInfo("SA", "Saudi Arabia", null)));
        when(likeCardSupplierAdapter.fetchProducts(any(), eq("20"), eq("SA"))).thenReturn(List.of(saudiProduct));

        stubUsdNormalization(new BigDecimal("14.50"));
        when(catalogPackagePort.resolvePackageId("SA", 10, DataUnit.GB, 15)).thenReturn("pkg-sa");
        when(catalogPackagePort.markUnavailableExcept(Set.of("pkg-sa"))).thenReturn(0);

        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        assertThat(result.productsFetched()).isEqualTo(1);
        verify(likeCardSupplierAdapter).fetchProducts(any(), eq("20"), eq("SA"));
    }

    @Test
    void createsAuditLogOnSuccessfulSync() {
        RawSupplierProduct product = new RawSupplierProduct(
                "5653", "JO", new BigDecimal("9.99"), "USD", 20, DataUnit.GB, 30);
        stubHarvest(List.of(product));
        stubUsdNormalization(new BigDecimal("9.99"));
        when(catalogPackagePort.resolvePackageId("JO", 20, DataUnit.GB, 30)).thenReturn("pkg-uuid-1");
        when(catalogPackagePort.markUnavailableExcept(any())).thenReturn(0);

        useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        ArgumentCaptor<SupplierSyncAuditLog> captor = ArgumentCaptor.forClass(SupplierSyncAuditLog.class);
        verify(auditLogPort, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        SupplierSyncAuditLog finalLog = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalLog.getStatus()).isIn(
                SupplierSyncAuditStatus.SUCCESS,
                SupplierSyncAuditStatus.PARTIAL);
        assertThat(finalLog.getTotalProcessed()).isEqualTo(1);
    }

    @Test
    void invalidatesCacheAfterSuccessfulSync() {
        RawSupplierProduct product = new RawSupplierProduct(
                "5653", "JO", new BigDecimal("9.99"), "USD", 20, DataUnit.GB, 30);
        stubHarvest(List.of(product));
        stubUsdNormalization(new BigDecimal("9.99"));
        when(catalogPackagePort.resolvePackageId("JO", 20, DataUnit.GB, 30)).thenReturn("pkg-uuid-1");
        when(catalogPackagePort.markUnavailableExcept(any())).thenReturn(0);

        useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        verify(cacheInvalidator).invalidateAll();
    }

    @Test
    void doesNotInvalidateCacheOnFailedSync() {
        when(credentialsPort.getCredentials("LIKE_CARD")).thenThrow(
                new IllegalArgumentException("No credentials"));

        try {
            useCase.execute(SyncSupplierCatalogCommand.forLikeCard());
        } catch (IllegalArgumentException ignored) {
        }

        verify(cacheInvalidator, never()).invalidateAll();
    }

    @Test
    void createsAuditLogOnFailedSync() {
        when(credentialsPort.getCredentials("LIKE_CARD")).thenThrow(
                new IllegalArgumentException("No credentials"));

        try {
            useCase.execute(SyncSupplierCatalogCommand.forLikeCard());
        } catch (IllegalArgumentException ignored) {
        }

        ArgumentCaptor<SupplierSyncAuditLog> captor = ArgumentCaptor.forClass(SupplierSyncAuditLog.class);
        verify(auditLogPort, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        SupplierSyncAuditLog finalLog = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalLog.getStatus()).isEqualTo(SupplierSyncAuditStatus.FAILED);
        assertThat(finalLog.getErrorMessage()).contains("No credentials");
    }

    private static MappingUpsertResult createdUpsert(String catalogPackageId, String remoteProductId,
                                                     BigDecimal cost, String currency,
                                                     BigDecimal normalized, String normalizedCurrency) {
        return new MappingUpsertResult(
                true, false, catalogPackageId, remoteProductId,
                null, cost, null, currency,
                null, normalized, null, normalizedCurrency);
    }

    private void stubHarvest(List<RawSupplierProduct> products) {
        when(likeCardSupplierAdapter.fetchCategoryIds(any())).thenReturn(List.of("10"));
        when(likeCardSupplierAdapter.fetchCountries(any(), eq("10"))).thenReturn(List.of(
                new CountryInfo("JO", "Jordan", null)));
        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("JO"))).thenReturn(products);
    }

    private void stubUsdNormalization(BigDecimal amount) {
        when(currencyNormalizationService.normalize(amount, "USD"))
                .thenReturn(new NormalizedCost(amount, "USD", amount, "USD"));
    }
}
