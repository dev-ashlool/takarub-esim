package com.takarub.esim.catalog.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.takarub.esim.supplier.domain.model.DataUnit;

class CatalogSlugGeneratorTest {

    @Test
    void convertsEnglishNameToUrlSafeSlug() {
        assertThat(CatalogSlugGenerator.fromEnglishName("Saudi Arabia")).isEqualTo("saudi-arabia");
        assertThat(CatalogSlugGenerator.fromEnglishName("Jordan")).isEqualTo("jordan");
        assertThat(CatalogSlugGenerator.fromEnglishName("United States")).isEqualTo("united-states");
    }

    @Test
    void stripsSpecialCharactersAndCollapsesHyphens() {
        assertThat(CatalogSlugGenerator.fromEnglishName("St. Kitts & Nevis!!!"))
                .isEqualTo("st-kitts-nevis");
    }

    @Test
    void uniqueAppendsFallbackWhenBaseCollides() {
        Set<String> existing = new HashSet<>();
        existing.add("europe");

        String slug = CatalogSlugGenerator.unique("europe", "EU-REGION", existing::contains);

        assertThat(slug).isEqualTo("europe-eu-region");
        assertThat(existing.contains(slug)).isFalse();
    }
}

class PackageSlugFormatterTest {

    @Test
    void formatsStandardPackageSlug() {
        assertThat(PackageSlugFormatter.format("jordan", 10, DataUnit.GB, 30))
                .isEqualTo("jordan-10gb-30days");
    }

    @Test
    void formatsUnlimitedPackageSlugWithoutAmount() {
        assertThat(PackageSlugFormatter.format("jordan", 0, DataUnit.UNLIMITED, 7))
                .isEqualTo("jordan-unlimited-7days");
    }
}
