package com.takarub.esim.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PriceTest {

    @Test
    void acceptsPositiveAmountWithCurrency() {
        Price price = new Price(new BigDecimal("12.50"), Currency.getInstance("USD"));

        assertThat(price.amount()).isEqualByComparingTo("12.50");
        assertThat(price.currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01"})
    void rejectsNonPositiveAmount(String amount) {
        assertThatThrownBy(() -> new Price(new BigDecimal(amount), Currency.getInstance("SAR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount must be greater than zero");
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Price(null, Currency.getInstance("EUR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount must be greater than zero");
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> new Price(new BigDecimal("1.00"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currency must not be null");
    }
}
