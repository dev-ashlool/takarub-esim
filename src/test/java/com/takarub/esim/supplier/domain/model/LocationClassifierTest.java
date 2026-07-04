package com.takarub.esim.supplier.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LocationClassifierTest {

    @ParameterizedTest
    @ValueSource(strings = {"JO", "TR", "SA", "US", "AE"})
    void classifiesIsoAlpha2AsCountry(String code) {
        assertThat(LocationClassifier.classify(code)).isEqualTo(LocationClassifier.Classification.COUNTRY);
        assertThat(LocationClassifier.isValidCountryIso(code)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USA", "GBR", "JOR"})
    void classifiesIsoAlpha3AsCountry(String code) {
        assertThat(LocationClassifier.classify(code)).isEqualTo(LocationClassifier.Classification.COUNTRY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT-AZ", "US-CA"})
    void classifiesSubdivisionCodesAsCountry(String code) {
        assertThat(LocationClassifier.classify(code)).isEqualTo(LocationClassifier.Classification.COUNTRY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"North_America", "Latin_America", "Caribbean_Islands", "MiddleEast&Africa", "Middle_East"})
    void classifiesKnownPatternsAsRegion(String value) {
        assertThat(LocationClassifier.classify(value)).isEqualTo(LocationClassifier.Classification.REGION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void classifiesBlankAsInvalid(String value) {
        assertThat(LocationClassifier.classify(value)).isEqualTo(LocationClassifier.Classification.INVALID);
    }

    @Test
    void classifiesNullAsInvalid() {
        assertThat(LocationClassifier.classify(null)).isEqualTo(LocationClassifier.Classification.INVALID);
    }
}
