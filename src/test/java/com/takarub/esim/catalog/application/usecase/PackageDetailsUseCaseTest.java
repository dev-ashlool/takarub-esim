package com.takarub.esim.catalog.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.query.GetPackageDetailsQuery;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.catalog.domain.exceptions.PackageNotFoundException;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@ExtendWith(MockitoExtension.class)
class PackageDetailsUseCaseTest {

    @Mock
    private CatalogBrowsePort catalogBrowsePort;

    private PackageDetailsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PackageDetailsUseCase(catalogBrowsePort);
    }

    @Test
    void returnsPackageDetailsWhenPackageExists() {
        PackageDetailsView expected = new PackageDetailsView(
                "pkg-1", "JO", "الأردن", "Jordan",
                "https://cdn.example/jo.png", 5, DataUnit.GB, 7, true, LocationType.COUNTRY);
        when(catalogBrowsePort.findPackageById("pkg-1")).thenReturn(Optional.of(expected));

        PackageDetailsView result = useCase.execute(new GetPackageDetailsQuery("pkg-1"));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void throwsPackageNotFoundExceptionWhenPackageDoesNotExist() {
        when(catalogBrowsePort.findPackageById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new GetPackageDetailsQuery("nonexistent")))
                .isInstanceOf(PackageNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }
}
