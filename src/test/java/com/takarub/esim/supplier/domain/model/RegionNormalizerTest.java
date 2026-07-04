package com.takarub.esim.supplier.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegionNormalizerTest {

    @Test
    void replacesUnderscoresWithSpaces() {
        assertThat(RegionNormalizer.normalize("North_America")).isEqualTo("North America");
        assertThat(RegionNormalizer.normalize("Latin_America")).isEqualTo("Latin America");
        assertThat(RegionNormalizer.normalize("Caribbean_Islands")).isEqualTo("Caribbean Islands");
    }

    @Test
    void insertsSpacesBeforeCamelCase() {
        assertThat(RegionNormalizer.normalize("MiddleEast")).isEqualTo("Middle East");
    }

    @Test
    void normalizesAmpersandWithSpaces() {
        assertThat(RegionNormalizer.normalize("MiddleEast&Africa")).isEqualTo("Middle East & Africa");
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(RegionNormalizer.normalize(null)).isNull();
        assertThat(RegionNormalizer.normalize("")).isEmpty();
    }

    @Test
    void preservesSimpleValues() {
        assertThat(RegionNormalizer.normalize("Europe")).isEqualTo("Europe");
    }
}
