package com.takarub.esim.supplier.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.domain.port.ExchangeRatePort;
import com.takarub.esim.catalog.domain.service.CurrencyNormalizationService;
import com.takarub.esim.catalog.infrastructure.persistence.CatalogPackageEntity;
import com.takarub.esim.catalog.infrastructure.persistence.CountryEntity;
import com.takarub.esim.supplier.application.result.MappingUpsertResult;
import com.takarub.esim.supplier.domain.model.DataUnit;

@ExtendWith(MockitoExtension.class)
class SupplierPackageMappingAdapterUpsertChangeTest {

    @Mock
    private SupplierPackageMappingJpaRepository repository;

    @Mock
    private CatalogPackageEntityResolver catalogPackageEntityResolver;

    @Mock
    private ExchangeRatePort exchangeRatePort;

    private SupplierPackageMappingAdapter adapter;
    private CatalogPackageEntity catalogPackage;

    @BeforeEach
    void setUp() {
        adapter = new SupplierPackageMappingAdapter(
                repository,
                catalogPackageEntityResolver,
                new CurrencyNormalizationService(exchangeRatePort));
        CountryEntity country = new CountryEntity("JO", "الأردن", "Jordan", null);
        catalogPackage = new CatalogPackageEntity("pkg-1", country, 1, DataUnit.GB, 7, true);
        when(catalogPackageEntityResolver.requireById("pkg-1")).thenReturn(catalogPackage);
    }

    @Test
    void firstUpsertIsCreatedChange() {
        when(repository.findBySupplierKeyAndRemoteProductId("LIKE_CARD", "5653")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        MappingUpsertResult result = adapter.upsertInStock(
                "pkg-1", "LIKE_CARD", "5653",
                new BigDecimal("4.71"), "USD",
                new BigDecimal("4.71"), "USD");

        assertThat(result.created()).isTrue();
        assertThat(result.costChanged()).isFalse();
        assertThat(result.shouldLogChange()).isTrue();
        assertThat(result.newCostPrice()).isEqualByComparingTo("4.71");
    }

    @Test
    void priceChangeIsDetectedAsCostUpdated() {
        SupplierPackageMappingEntity existing = new SupplierPackageMappingEntity(
                catalogPackage, "LIKE_CARD", "5653",
                new BigDecimal("4.71"), "USD",
                new BigDecimal("4.71"), "USD", true);
        when(repository.findBySupplierKeyAndRemoteProductId("LIKE_CARD", "5653"))
                .thenReturn(Optional.of(existing));
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        MappingUpsertResult result = adapter.upsertInStock(
                "pkg-1", "LIKE_CARD", "5653",
                new BigDecimal("5.20"), "USD",
                new BigDecimal("5.20"), "USD");

        assertThat(result.created()).isFalse();
        assertThat(result.costChanged()).isTrue();
        assertThat(result.oldCostPrice()).isEqualByComparingTo("4.71");
        assertThat(result.newCostPrice()).isEqualByComparingTo("5.20");
        assertThat(existing.getCostPrice()).isEqualByComparingTo("5.20");
    }

    @Test
    void identicalCostIsNotLoggedAsChange() {
        SupplierPackageMappingEntity existing = new SupplierPackageMappingEntity(
                catalogPackage, "LIKE_CARD", "5653",
                new BigDecimal("4.7100"), "USD",
                new BigDecimal("4.7100"), "USD", true);
        when(repository.findBySupplierKeyAndRemoteProductId("LIKE_CARD", "5653"))
                .thenReturn(Optional.of(existing));
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        MappingUpsertResult result = adapter.upsertInStock(
                "pkg-1", "LIKE_CARD", "5653",
                new BigDecimal("4.71"), "USD",
                new BigDecimal("4.71"), "USD");

        assertThat(result.created()).isFalse();
        assertThat(result.costChanged()).isFalse();
        assertThat(result.shouldLogChange()).isFalse();
    }
}
