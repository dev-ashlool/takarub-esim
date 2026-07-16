package com.takarub.esim.supplier.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.domain.port.ExchangeRatePort;
import com.takarub.esim.catalog.domain.service.CurrencyNormalizationService;
import com.takarub.esim.catalog.infrastructure.persistence.CatalogPackageEntity;
import com.takarub.esim.catalog.infrastructure.persistence.CountryEntity;
import com.takarub.esim.supplier.domain.model.DataUnit;

@ExtendWith(MockitoExtension.class)
class SupplierPackageMappingAdapterRecalculationTest {

    @Mock
    private SupplierPackageMappingJpaRepository repository;

    @Mock
    private CatalogPackageEntityResolver catalogPackageEntityResolver;

    @Mock
    private ExchangeRatePort exchangeRatePort;

    private SupplierPackageMappingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SupplierPackageMappingAdapter(
                repository,
                catalogPackageEntityResolver,
                new CurrencyNormalizationService(exchangeRatePort));
    }

    @Test
    void recalculatesNormalizedOnlyAndLeavesOriginalCostUntouched() {
        CountryEntity country = new CountryEntity("SA", "السعودية", "Saudi Arabia", null);
        CatalogPackageEntity catalogPackage =
                new CatalogPackageEntity("pkg-1", country, 10, DataUnit.GB, 15, true);

        SupplierPackageMappingEntity sarMapping = new SupplierPackageMappingEntity(
                catalogPackage, "SUPPLIER_X", "1",
                new BigDecimal("37.5000"), "SAR",
                new BigDecimal("9.0000"), "USD",
                true);

        when(repository.findAllByCostCurrencyIgnoreCase("SAR")).thenReturn(List.of(sarMapping));

        int updated = adapter.recalculateNormalizedCosts("SAR", new BigDecimal("0.26660000"));

        assertThat(updated).isEqualTo(1);
        assertThat(sarMapping.getCostPrice()).isEqualByComparingTo("37.5000");
        assertThat(sarMapping.getCostCurrency()).isEqualTo("SAR");
        assertThat(sarMapping.getNormalizedCostPrice()).isEqualByComparingTo("9.9975");
        assertThat(sarMapping.getNormalizedCurrency()).isEqualTo("USD");
        verify(repository).save(sarMapping);
    }

    @Test
    void usdOriginalCurrencyIsNoOp() {
        int updated = adapter.recalculateNormalizedCosts("USD", BigDecimal.ONE);

        assertThat(updated).isZero();
        verify(repository, never()).findAllByCostCurrencyIgnoreCase(any());
        verify(repository, never()).save(any());
    }
}
