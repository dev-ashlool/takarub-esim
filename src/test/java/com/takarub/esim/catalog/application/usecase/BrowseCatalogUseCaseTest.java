package com.takarub.esim.catalog.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.query.BrowseCatalogQuery;
import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@ExtendWith(MockitoExtension.class)
class BrowseCatalogUseCaseTest {

    @Mock
    private CatalogBrowsePort catalogBrowsePort;

    private BrowseCatalogUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BrowseCatalogUseCase(catalogBrowsePort);
    }

    @Test
    void returnsAllAvailablePackagesWhenCountryFilterIsAbsent() {
        CatalogPackageView view = sampleView("pkg-1", "JO");
        when(catalogBrowsePort.findAvailablePackages(null)).thenReturn(List.of(view));

        List<CatalogPackageView> result = useCase.execute(new BrowseCatalogQuery(null));

        assertThat(result).containsExactly(view);
        verify(catalogBrowsePort).findAvailablePackages(null);
    }

    @Test
    void normalizesCountryFilterToUppercase() {
        when(catalogBrowsePort.findAvailablePackages("JO")).thenReturn(List.of());

        useCase.execute(new BrowseCatalogQuery(" jo "));

        verify(catalogBrowsePort).findAvailablePackages("JO");
    }

    @Test
    void treatsBlankCountryFilterAsAbsent() {
        when(catalogBrowsePort.findAvailablePackages(null)).thenReturn(List.of());

        useCase.execute(new BrowseCatalogQuery("   "));

        verify(catalogBrowsePort).findAvailablePackages(null);
    }

    @Test
    void returnsEmptyListWhenCatalogIsEmpty() {
        when(catalogBrowsePort.findAvailablePackages(null)).thenReturn(List.of());

        assertThat(useCase.execute(new BrowseCatalogQuery(null))).isEmpty();
    }

    private static CatalogPackageView sampleView(String id, String countryIso) {
        return new CatalogPackageView(
                id, countryIso, "الأردن", "Jordan", null, 5, DataUnit.GB, 7, LocationType.COUNTRY,
                new java.math.BigDecimal("12.00"), "USD");
    }
}
