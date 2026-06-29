package com.takarub.esim.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.supplier.domain.model.DataUnit;

@DataJpaTest
@Import(CatalogBrowseAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogBrowseAdapterTest {

    @Autowired
    private CatalogBrowseAdapter catalogBrowseAdapter;

    @Autowired
    private CatalogPackageJpaRepository catalogPackageJpaRepository;

    @Autowired
    private CountryJpaRepository countryJpaRepository;

    private CountryEntity jordan;
    private CountryEntity saudiArabia;

    @BeforeEach
    void setUpCountries() {
        jordan = countryJpaRepository.save(new CountryEntity("JO", "الأردن", "Jordan", null));
        saudiArabia = countryJpaRepository.save(new CountryEntity("SA", "السعودية", "Saudi Arabia", null));
    }

    @Test
    void returnsOnlyAvailablePackages() {
        persistPackage(jordan, 5, DataUnit.GB, 7, true);
        persistPackage(jordan, 10, DataUnit.GB, 30, false);

        assertThat(catalogBrowseAdapter.findAvailablePackages(null)).hasSize(1);
    }

    @Test
    void filtersPackagesByCountryIso() {
        persistPackage(jordan, 5, DataUnit.GB, 7, true);
        persistPackage(saudiArabia, 3, DataUnit.GB, 15, true);

        assertThat(catalogBrowseAdapter.findAvailablePackages("JO"))
                .extracting(CatalogPackageView::countryIso)
                .containsExactly("JO");
    }

    @Test
    void returnsEmptyListWhenNoPackagesMatchFilter() {
        persistPackage(jordan, 5, DataUnit.GB, 7, true);

        assertThat(catalogBrowseAdapter.findAvailablePackages("SA")).isEmpty();
    }

    @Test
    void returnsEmptyListWhenCatalogIsEmpty() {
        assertThat(catalogBrowseAdapter.findAvailablePackages(null)).isEmpty();
    }

    @Test
    void mapsCountryMetadataIntoBrowseView() {
        String packageId = persistPackage(jordan, 5, DataUnit.GB, 7, true);

        var view = catalogBrowseAdapter.findAvailablePackages("JO").getFirst();

        assertThat(view.id()).isEqualTo(packageId);
        assertThat(view.countryArabicName()).isEqualTo("الأردن");
        assertThat(view.countryEnglishName()).isEqualTo("Jordan");
        assertThat(view.dataAmount()).isEqualTo(5);
        assertThat(view.dataUnit()).isEqualTo(DataUnit.GB);
        assertThat(view.durationDays()).isEqualTo(7);
    }

    private String persistPackage(
            CountryEntity country, int dataAmount, DataUnit dataUnit, int durationDays, boolean available) {
        String packageId = UUID.randomUUID().toString();
        catalogPackageJpaRepository.save(
                new CatalogPackageEntity(packageId, country, dataAmount, dataUnit, durationDays, available));
        return packageId;
    }
}
