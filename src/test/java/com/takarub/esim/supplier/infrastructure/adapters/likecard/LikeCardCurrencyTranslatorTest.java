package com.takarub.esim.supplier.infrastructure.adapters.likecard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LikeCardCurrencyTranslatorTest {

    @Test
    void translatesArabicDollarLabelToUsd() {
        assertThat(LikeCardCurrencyTranslator.toIsoCurrency("دولار")).isEqualTo("USD");
    }

    @Test
    void translatesArabicRiyalLabelToSar() {
        assertThat(LikeCardCurrencyTranslator.toIsoCurrency("ريال")).isEqualTo("SAR");
    }

    @Test
    void passesThroughExistingIsoCode() {
        assertThat(LikeCardCurrencyTranslator.toIsoCurrency("eur")).isEqualTo("EUR");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "", "  "})
    void rejectsBlankCurrency(String currency) {
        assertThatThrownBy(() -> LikeCardCurrencyTranslator.toIsoCurrency(currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currency must not be blank");
    }
}
