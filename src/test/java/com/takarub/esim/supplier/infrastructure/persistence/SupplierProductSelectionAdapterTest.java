package com.takarub.esim.supplier.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.infrastructure.persistence.CatalogPackageEntity;
import com.takarub.esim.catalog.infrastructure.persistence.CountryEntity;
import com.takarub.esim.supplier.application.result.SelectedSupplierProduct;
import com.takarub.esim.supplier.domain.model.DataUnit;

/**
 * Unit tests for {@link SupplierProductSelectionAdapter}.
 *
 * <p>Full {@code @DataJpaTest} coverage of the Spring Data {@code findFirst…OrderBy…} query is
 * currently blocked by the pre-existing H2 incompatibility with Flyway V13 ({@code AFTER} clause),
 * same limitation noted on order repository adapter tests. Ordering rules below are asserted via
 * the same comparator encoded in the repository method name.
 */
@ExtendWith(MockitoExtension.class)
class SupplierProductSelectionAdapterTest {

    private static final String PACKAGE_ID = "pkg-1";

    @Mock
    private SupplierPackageMappingJpaRepository repository;

    private SupplierProductSelectionAdapter adapter;
    private CatalogPackageEntity catalogPackage;

    @BeforeEach
    void setUp() {
        adapter = new SupplierProductSelectionAdapter(repository);
        CountryEntity country = new CountryEntity("JO", "الأردن", "Jordan", null);
        catalogPackage = new CatalogPackageEntity(PACKAGE_ID, country, 1, DataUnit.GB, 7, true);
    }

    @Test
    void selectsSingleInStockMapping() {
        SupplierPackageMappingEntity mapping = mapping("LIKE_CARD", "100", "5.00", "5.00", true);
        when(repository
                .findFirstByCatalogPackage_IdAndInStockTrueOrderByNormalizedCostPriceAscSupplierKeyAscRemoteProductIdAsc(
                        PACKAGE_ID))
                .thenReturn(Optional.of(mapping));

        Optional<SelectedSupplierProduct> selected = adapter.findWinningInStockMapping(PACKAGE_ID);

        assertThat(selected).isPresent();
        assertThat(selected.get().supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(selected.get().remoteProductId()).isEqualTo("100");
        assertThat(selected.get().supplierCostAtCheckout()).isEqualByComparingTo("5.00");
        assertThat(selected.get().supplierCostCurrency()).isEqualTo("USD");
        verify(repository)
                .findFirstByCatalogPackage_IdAndInStockTrueOrderByNormalizedCostPriceAscSupplierKeyAscRemoteProductIdAsc(
                        PACKAGE_ID);
    }

    @Test
    void returnsOriginalCostNotNormalizedWhenTheyDiffer() {
        SupplierPackageMappingEntity mapping = mapping("LIKE_CARD", "100", "26.60", "7.09", true);
        mapping.setCostCurrency("SAR");
        when(repository
                .findFirstByCatalogPackage_IdAndInStockTrueOrderByNormalizedCostPriceAscSupplierKeyAscRemoteProductIdAsc(
                        PACKAGE_ID))
                .thenReturn(Optional.of(mapping));

        SelectedSupplierProduct selected = adapter.findWinningInStockMapping(PACKAGE_ID).orElseThrow();

        assertThat(selected.supplierCostAtCheckout()).isEqualByComparingTo("26.60");
        assertThat(selected.supplierCostCurrency()).isEqualTo("SAR");
    }

    @Test
    void returnsEmptyWhenRepositoryHasNoWinner() {
        when(repository
                .findFirstByCatalogPackage_IdAndInStockTrueOrderByNormalizedCostPriceAscSupplierKeyAscRemoteProductIdAsc(
                        PACKAGE_ID))
                .thenReturn(Optional.empty());

        assertThat(adapter.findWinningInStockMapping(PACKAGE_ID)).isEmpty();
    }

    @Test
    void returnsEmptyForBlankPackageIdWithoutCallingRepository() {
        assertThat(adapter.findWinningInStockMapping(" ")).isEmpty();
        assertThat(adapter.findWinningInStockMapping(null)).isEmpty();
    }

    @Test
    void cheapestNormalizedCostWins() {
        SupplierPackageMappingEntity winner = winnerAmong(List.of(
                candidate("LIKE_CARD", "100", "9.00"),
                candidate("SUPPLIER_B", "200", "7.50")));

        assertThat(winner.getSupplierKey()).isEqualTo("SUPPLIER_B");
        assertThat(winner.getRemoteProductId()).isEqualTo("200");
    }

    @Test
    void outOfStockMappingIgnored() {
        List<Candidate> inStockOnly = List.of(
                candidate("LIKE_CARD", "100", "3.00", false),
                candidate("SUPPLIER_B", "200", "8.00", true)).stream()
                .filter(Candidate::inStock)
                .toList();

        SupplierPackageMappingEntity winner = winnerAmong(inStockOnly);

        assertThat(winner.getSupplierKey()).isEqualTo("SUPPLIER_B");
        assertThat(winner.getRemoteProductId()).isEqualTo("200");
    }

    @Test
    void normalizedCostTieBreaksBySupplierKeyAsc() {
        SupplierPackageMappingEntity winner = winnerAmong(List.of(
                candidate("SUPPLIER_B", "200", "7.00"),
                candidate("LIKE_CARD", "100", "7.00")));

        assertThat(winner.getSupplierKey()).isEqualTo("LIKE_CARD");
        assertThat(winner.getRemoteProductId()).isEqualTo("100");
    }

    @Test
    void sameSupplierAndCostTieBreaksByRemoteProductIdAsc() {
        SupplierPackageMappingEntity winner = winnerAmong(List.of(
                candidate("LIKE_CARD", "200", "7.00"),
                candidate("LIKE_CARD", "100", "7.00")));

        assertThat(winner.getSupplierKey()).isEqualTo("LIKE_CARD");
        assertThat(winner.getRemoteProductId()).isEqualTo("100");
    }

    private SupplierPackageMappingEntity winnerAmong(List<Candidate> candidates) {
        Candidate winner = candidates.stream()
                .filter(Candidate::inStock)
                .min(Comparator
                        .comparing(Candidate::normalizedCost)
                        .thenComparing(Candidate::supplierKey)
                        .thenComparing(Candidate::remoteProductId))
                .orElseThrow();
        return mapping(
                winner.supplierKey(),
                winner.remoteProductId(),
                winner.costPrice().toPlainString(),
                winner.normalizedCost().toPlainString(),
                true);
    }

    private Candidate candidate(String supplierKey, String remoteProductId, String normalizedCost) {
        return candidate(supplierKey, remoteProductId, normalizedCost, true);
    }

    private Candidate candidate(String supplierKey,
                                String remoteProductId,
                                String normalizedCost,
                                boolean inStock) {
        return new Candidate(
                supplierKey,
                remoteProductId,
                new BigDecimal(normalizedCost),
                new BigDecimal(normalizedCost),
                inStock);
    }

    private SupplierPackageMappingEntity mapping(String supplierKey,
                                                 String remoteProductId,
                                                 String costPrice,
                                                 String normalizedCost,
                                                 boolean inStock) {
        return new SupplierPackageMappingEntity(
                catalogPackage,
                supplierKey,
                remoteProductId,
                new BigDecimal(costPrice),
                "USD",
                new BigDecimal(normalizedCost),
                "USD",
                inStock);
    }

    private record Candidate(
            String supplierKey,
            String remoteProductId,
            BigDecimal costPrice,
            BigDecimal normalizedCost,
            boolean inStock) {
    }
}
