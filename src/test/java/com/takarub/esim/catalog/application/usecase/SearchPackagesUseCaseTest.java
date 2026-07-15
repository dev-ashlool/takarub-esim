package com.takarub.esim.catalog.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.query.SearchPackagesQuery;
import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.catalog.application.result.PagedResult;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@ExtendWith(MockitoExtension.class)
class SearchPackagesUseCaseTest {

    @Mock
    private CatalogBrowsePort catalogBrowsePort;

    private SearchPackagesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchPackagesUseCase(catalogBrowsePort);
    }

    @Test
    void delegatesToPortWithTrimmedTerm() {
        CatalogPackageView view = new CatalogPackageView(
                "pkg-1", "JO", "الأردن", "Jordan", null, 5, DataUnit.GB, 7, LocationType.COUNTRY,
                new java.math.BigDecimal("12.00"), "USD", "jordan");
        PagedResult<CatalogPackageView> expected = new PagedResult<>(List.of(view), 0, 20, 1, 1);
        when(catalogBrowsePort.searchAvailablePackages("Jordan", null, null, null, null, 0, 20))
                .thenReturn(expected);

        PagedResult<CatalogPackageView> result = useCase.execute(
                new SearchPackagesQuery("  Jordan  ", null, null, null, null, 0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().countryEnglishName()).isEqualTo("Jordan");
        verify(catalogBrowsePort).searchAvailablePackages("Jordan", null, null, null, null, 0, 20);
    }

    @Test
    void returnsEmptyResultForBlankSearchTerm() {
        PagedResult<CatalogPackageView> result = useCase.execute(
                new SearchPackagesQuery("   ", null, null, null, null, 0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        verifyNoInteractions(catalogBrowsePort);
    }

    @Test
    void returnsEmptyResultForNullSearchTerm() {
        PagedResult<CatalogPackageView> result = useCase.execute(
                new SearchPackagesQuery(null, null, null, null, null, 0, 20));

        assertThat(result.content()).isEmpty();
        verifyNoInteractions(catalogBrowsePort);
    }

    @Test
    void clampsPageSizeToMaximum() {
        PagedResult<CatalogPackageView> expected = new PagedResult<>(List.of(), 0, 100, 0, 0);
        when(catalogBrowsePort.searchAvailablePackages("test", null, null, null, null, 0, 100))
                .thenReturn(expected);

        useCase.execute(new SearchPackagesQuery("test", null, null, null, null, 0, 500));

        verify(catalogBrowsePort).searchAvailablePackages("test", null, null, null, null, 0, 100);
    }

    @Test
    void clampsNegativePageToZero() {
        PagedResult<CatalogPackageView> expected = new PagedResult<>(List.of(), 0, 20, 0, 0);
        when(catalogBrowsePort.searchAvailablePackages("test", null, null, null, null, 0, 20))
                .thenReturn(expected);

        useCase.execute(new SearchPackagesQuery("test", null, null, null, null, -1, 20));

        verify(catalogBrowsePort).searchAvailablePackages("test", null, null, null, null, 0, 20);
    }

    @Test
    void searchesByFiltersOnlyWhenNoSearchTerm() {
        PagedResult<CatalogPackageView> expected = new PagedResult<>(List.of(), 0, 20, 0, 0);
        when(catalogBrowsePort.searchAvailablePackages(null, "JO", 3, "GB", 30, 0, 20))
                .thenReturn(expected);

        useCase.execute(new SearchPackagesQuery(null, "JO", 3, "GB", 30, 0, 20));

        verify(catalogBrowsePort).searchAvailablePackages(null, "JO", 3, "GB", 30, 0, 20);
    }
}
