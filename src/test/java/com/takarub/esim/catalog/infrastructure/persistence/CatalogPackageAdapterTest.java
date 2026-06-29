package com.takarub.esim.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.takarub.esim.catalog.domain.exceptions.PackageNotFoundException;
import com.takarub.esim.supplier.domain.model.DataUnit;

@DataJpaTest
@Import(CatalogPackageAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogPackageAdapterTest {

    @Autowired
    private CatalogPackageAdapter catalogPackageAdapter;

    @Autowired
    private CatalogPackageJpaRepository catalogPackageJpaRepository;

    @Autowired
    private CountryJpaRepository countryJpaRepository;

    @Test
    void resolvePackageIdCreatesPackageWhenCountryExists() {
        countryJpaRepository.save(new CountryEntity("JO", "الأردن", "Jordan", null));

        String firstId = catalogPackageAdapter.resolvePackageId("JO", 5, DataUnit.GB, 7);
        String secondId = catalogPackageAdapter.resolvePackageId("JO", 5, DataUnit.GB, 7);

        assertThat(firstId).isEqualTo(secondId);
        assertThat(catalogPackageJpaRepository.findAll()).hasSize(1);
    }

    @Test
    void resolvePackageIdAutoProvisionsMissingCountry() {
        String packageId = catalogPackageAdapter.resolvePackageId("AE", 3, DataUnit.GB, 15);

        assertThat(countryJpaRepository.findById("AE")).isPresent();
        assertThat(catalogPackageJpaRepository.findById(packageId)).isPresent();
    }

    @Test
    void markUnavailableExceptHidesPackagesFromAvailability() {
        countryJpaRepository.save(new CountryEntity("JO", "الأردن", "Jordan", null));
        String availableId = catalogPackageAdapter.resolvePackageId("JO", 5, DataUnit.GB, 7);
        String unavailableId = catalogPackageAdapter.resolvePackageId("JO", 10, DataUnit.GB, 30);

        catalogPackageAdapter.markUnavailableExcept(Set.of(availableId));

        assertThat(catalogPackageJpaRepository.findById(availableId).orElseThrow().isAvailable()).isTrue();
        assertThat(catalogPackageJpaRepository.findById(unavailableId).orElseThrow().isAvailable()).isFalse();
    }

    @Test
    void markAvailableRestoresPackageVisibility() {
        countryJpaRepository.save(new CountryEntity("SA", "السعودية", "Saudi Arabia", null));
        String packageId = catalogPackageAdapter.resolvePackageId("SA", 1, DataUnit.GB, 1);
        catalogPackageAdapter.markUnavailableExcept(Set.of());

        catalogPackageAdapter.markAvailable(Set.of(packageId));

        assertThat(catalogPackageJpaRepository.findById(packageId).orElseThrow().isAvailable()).isTrue();
    }

    @Test
    void markAvailableThrowsWhenPackageDoesNotExist() {
        assertThatThrownBy(() -> catalogPackageAdapter.markAvailable(Set.of(UUID.randomUUID().toString())))
                .isInstanceOf(PackageNotFoundException.class);
    }
}
