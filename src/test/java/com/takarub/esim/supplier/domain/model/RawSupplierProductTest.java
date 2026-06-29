package com.takarub.esim.supplier.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RawSupplierProductTest {

    @Test
    void acceptsValidProductValues() {
        RawSupplierProduct product = validProduct();

        assertThat(product.id()).isEqualTo("5653");
        assertThat(product.countryIso()).isEqualTo("JO");
        assertThat(product.costPrice()).isEqualByComparingTo("9.99");
        assertThat(product.costCurrency()).isEqualTo("USD");
        assertThat(product.dataAmount()).isEqualTo(20);
        assertThat(product.dataUnit()).isEqualTo(DataUnit.GB);
        assertThat(product.durationDays()).isEqualTo(30);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01"})
    void rejectsNonPositiveCostPrice(String costPrice) {
        assertThatThrownBy(() -> new RawSupplierProduct(
                "5653",
                "JO",
                new BigDecimal(costPrice),
                "USD",
                1,
                DataUnit.GB,
                7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("costPrice must be greater than zero");
    }

    @Test
    void rejectsNullCostPrice() {
        assertThatThrownBy(() -> new RawSupplierProduct(
                "5653",
                "JO",
                null,
                "USD",
                1,
                DataUnit.GB,
                7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("costPrice must be greater than zero");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidDataAmount(int dataAmount) {
        assertThatThrownBy(() -> new RawSupplierProduct(
                "5653",
                "JO",
                new BigDecimal("9.99"),
                "USD",
                dataAmount,
                DataUnit.MB,
                7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dataAmount must be at least 1");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidDurationDays(int durationDays) {
        assertThatThrownBy(() -> new RawSupplierProduct(
                "5653",
                "EuropePlus",
                new BigDecimal("12.50"),
                "SAR",
                5,
                DataUnit.GB,
                durationDays))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("durationDays must be at least 1");
    }

    private static RawSupplierProduct validProduct() {
        return new RawSupplierProduct(
                "5653",
                "JO",
                new BigDecimal("9.99"),
                "USD",
                20,
                DataUnit.GB,
                30);
    }
}
