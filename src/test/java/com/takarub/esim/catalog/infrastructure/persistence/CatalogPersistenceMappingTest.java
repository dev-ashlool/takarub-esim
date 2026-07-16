package com.takarub.esim.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.infrastructure.persistence.SupplierPackageMappingEntity;

/**
 * Verifies catalog and supplier JPA entities map correctly onto the Flyway-built MySQL schema.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogPersistenceMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsAndReloadsCountryCatalogPackageAndSupplierMapping() {
        CountryEntity country = new CountryEntity("JO", "الأردن", "Jordan", "https://cdn.example/flags/jo.png");
        entityManager.persist(country);

        String packageId = UUID.randomUUID().toString();
        CatalogPackageEntity catalogPackage = new CatalogPackageEntity(
                packageId, country, 20, DataUnit.GB, 30, true);
        entityManager.persist(catalogPackage);

        SupplierPackageMappingEntity mapping = new SupplierPackageMappingEntity(
                catalogPackage,
                "LIKE_CARD",
                "5653",
                new BigDecimal("9.9900"),
                "USD",
                true);
        entityManager.persist(mapping);
        entityManager.flush();
        Integer mappingId = mapping.getId();
        entityManager.clear();

        CountryEntity reloadedCountry = entityManager.find(CountryEntity.class, "JO");
        CatalogPackageEntity reloadedPackage = entityManager.find(CatalogPackageEntity.class, packageId);
        SupplierPackageMappingEntity reloadedMapping =
                entityManager.find(SupplierPackageMappingEntity.class, mappingId);

        assertThat(reloadedCountry).isNotNull();
        assertThat(reloadedCountry.getArabicName()).isEqualTo("الأردن");
        assertThat(reloadedCountry.getEnglishName()).isEqualTo("Jordan");
        assertThat(reloadedCountry.getFlagImageUrl()).isEqualTo("https://cdn.example/flags/jo.png");

        assertThat(reloadedPackage).isNotNull();
        assertThat(reloadedPackage.getCountry().getId()).isEqualTo("JO");
        assertThat(reloadedPackage.getDataAmount()).isEqualTo(20);
        assertThat(reloadedPackage.getDataUnit()).isEqualTo(DataUnit.GB);
        assertThat(reloadedPackage.getDurationDays()).isEqualTo(30);
        assertThat(reloadedPackage.isAvailable()).isTrue();

        assertThat(reloadedMapping).isNotNull();
        assertThat(reloadedMapping.getCatalogPackage().getId()).isEqualTo(packageId);
        assertThat(reloadedMapping.getSupplierKey()).isEqualTo("LIKE_CARD");
        assertThat(reloadedMapping.getRemoteProductId()).isEqualTo("5653");
        assertThat(reloadedMapping.getCostPrice()).isEqualByComparingTo("9.9900");
        assertThat(reloadedMapping.getCostCurrency()).isEqualTo("USD");
        assertThat(reloadedMapping.isInStock()).isTrue();
    }
}
