package com.takarub.esim.supplier.application.usecases;



import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;



import java.math.BigDecimal;

import java.time.Instant;

import java.util.List;

import java.util.Map;

import java.util.Set;

import java.util.function.Supplier;



import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;



import com.takarub.esim.catalog.application.port.CatalogPackagePort;

import com.takarub.esim.identity.application.port.TransactionRunner;

import com.takarub.esim.supplier.application.command.SyncSupplierCatalogCommand;

import com.takarub.esim.supplier.application.port.SupplierCredentialsPort;

import com.takarub.esim.supplier.application.port.SupplierLikeCardProductLogPort;

import com.takarub.esim.supplier.application.port.SupplierPackageMappingPort;

import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;

import com.takarub.esim.supplier.domain.model.DataUnit;

import com.takarub.esim.supplier.domain.model.RawSupplierProduct;

import com.takarub.esim.supplier.domain.model.SupplierType;

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



    private SyncSupplierCatalogUseCase useCase;



    @BeforeEach

    void setUp() {

        useCase = new SyncSupplierCatalogUseCase(

                transactionRunner,

                credentialsPort,

                likeCardSupplierAdapter,

                likeCardProductLogPort,

                catalogPackagePort,

                packageMappingPort);

        when(transactionRunner.execute(any())).thenAnswer(invocation -> {

            Supplier<?> work = invocation.getArgument(0);

            return work.get();

        });

        when(likeCardSupplierAdapter.getSupplierType()).thenReturn(SupplierType.LIKE_CARD);

        when(credentialsPort.getCredentials("LIKE_CARD")).thenReturn(Map.of("base_url", "https://api.example"));

    }



    @Test

    void synchronizesPresentProductsAndAppliesPresenceRules() {

        RawSupplierProduct product = new RawSupplierProduct(

                "5653", "JO", new BigDecimal("9.99"), "USD", 20, DataUnit.GB, 30);

        stubHarvest(List.of(product));



        when(catalogPackagePort.resolvePackageId("JO", 20, DataUnit.GB, 30)).thenReturn("pkg-uuid-1");

        when(packageMappingPort.markOutOfStockExcept(eq("LIKE_CARD"), any())).thenReturn(2);

        when(catalogPackagePort.markUnavailableExcept(Set.of("pkg-uuid-1"))).thenReturn(1);



        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());



        assertThat(result.supplierKey()).isEqualTo("LIKE_CARD");

        assertThat(result.productsFetched()).isEqualTo(1);

        assertThat(result.mappingsUpserted()).isEqualTo(1);

        assertThat(result.mappingsMarkedOutOfStock()).isEqualTo(2);

        assertThat(result.catalogPackagesMarkedUnavailable()).isEqualTo(1);



        verify(likeCardProductLogPort).saveOrUpdate(eq(product), any(Instant.class));

        verify(packageMappingPort).upsertInStock(

                "pkg-uuid-1", "LIKE_CARD", "5653", new BigDecimal("9.99"), "USD");

        verify(catalogPackagePort).markAvailable(Set.of("pkg-uuid-1"));



        ArgumentCaptor<Set<String>> presentCaptor = ArgumentCaptor.forClass(Set.class);

        verify(packageMappingPort).markOutOfStockExcept(eq("LIKE_CARD"), presentCaptor.capture());

        assertThat(presentCaptor.getValue()).containsExactly("5653");

    }



    @Test

    void synchronizesProductsAcrossMultipleCategoriesAndCountries() {

        RawSupplierProduct jordanProduct = new RawSupplierProduct(

                "1001", "JO", new BigDecimal("9.99"), "USD", 5, DataUnit.GB, 7);

        RawSupplierProduct saudiProduct = new RawSupplierProduct(

                "2001", "SA", new BigDecimal("14.50"), "USD", 10, DataUnit.GB, 15);



        when(likeCardSupplierAdapter.fetchCategoryIds(any())).thenReturn(List.of("10", "20"));

        when(likeCardSupplierAdapter.fetchCountryIsos(any(), eq("10"))).thenReturn(List.of("JO", "AE"));

        when(likeCardSupplierAdapter.fetchCountryIsos(any(), eq("20"))).thenReturn(List.of("SA"));

        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("JO"))).thenReturn(List.of(jordanProduct));

        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("AE"))).thenReturn(List.of());

        when(likeCardSupplierAdapter.fetchProducts(any(), eq("20"), eq("SA"))).thenReturn(List.of(saudiProduct));



        when(catalogPackagePort.resolvePackageId("JO", 5, DataUnit.GB, 7)).thenReturn("pkg-jo");

        when(catalogPackagePort.resolvePackageId("SA", 10, DataUnit.GB, 15)).thenReturn("pkg-sa");

        when(packageMappingPort.markOutOfStockExcept(eq("LIKE_CARD"), any())).thenReturn(0);

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

        when(packageMappingPort.markOutOfStockExcept(eq("LIKE_CARD"), eq(Set.of()))).thenReturn(0);

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

        when(likeCardSupplierAdapter.fetchCountryIsos(any(), eq("10"))).thenReturn(List.of("JO", "SA"));

        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("JO")))

                .thenThrow(new SupplierApiException("LikeCard products API call failed"));

        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("SA"))).thenReturn(List.of(saudiProduct));



        when(catalogPackagePort.resolvePackageId("SA", 10, DataUnit.GB, 15)).thenReturn("pkg-sa");

        when(packageMappingPort.markOutOfStockExcept(eq("LIKE_CARD"), any())).thenReturn(0);

        when(catalogPackagePort.markUnavailableExcept(Set.of("pkg-sa"))).thenReturn(0);



        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());



        assertThat(result.productsFetched()).isEqualTo(1);

        verify(packageMappingPort).upsertInStock(

                "pkg-sa", "LIKE_CARD", "2001", new BigDecimal("14.50"), "USD");

    }



    @Test
    void continuesSynchronizationWhenOneCategoryCountriesCallFails() {
        RawSupplierProduct saudiProduct = new RawSupplierProduct(
                "2001", "SA", new BigDecimal("14.50"), "USD", 10, DataUnit.GB, 15);

        when(likeCardSupplierAdapter.fetchCategoryIds(any())).thenReturn(List.of("10", "20"));
        when(likeCardSupplierAdapter.fetchCountryIsos(any(), eq("10")))
                .thenThrow(new SupplierApiException("LikeCard countries API call failed"));
        when(likeCardSupplierAdapter.fetchCountryIsos(any(), eq("20"))).thenReturn(List.of("SA"));
        when(likeCardSupplierAdapter.fetchProducts(any(), eq("20"), eq("SA"))).thenReturn(List.of(saudiProduct));

        when(catalogPackagePort.resolvePackageId("SA", 10, DataUnit.GB, 15)).thenReturn("pkg-sa");
        when(packageMappingPort.markOutOfStockExcept(eq("LIKE_CARD"), any())).thenReturn(0);
        when(catalogPackagePort.markUnavailableExcept(Set.of("pkg-sa"))).thenReturn(0);

        var result = useCase.execute(SyncSupplierCatalogCommand.forLikeCard());

        assertThat(result.productsFetched()).isEqualTo(1);
        verify(likeCardSupplierAdapter).fetchProducts(any(), eq("20"), eq("SA"));
    }

    private void stubHarvest(List<RawSupplierProduct> products) {

        when(likeCardSupplierAdapter.fetchCategoryIds(any())).thenReturn(List.of("10"));

        when(likeCardSupplierAdapter.fetchCountryIsos(any(), eq("10"))).thenReturn(List.of("JO"));

        when(likeCardSupplierAdapter.fetchProducts(any(), eq("10"), eq("JO"))).thenReturn(products);

    }

}


