package com.takarub.esim.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class NormalizedCostTest {

    @Test
    void rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> new NormalizedCost(
                BigDecimal.ZERO, "USD", new BigDecimal("1.00"), "USD"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new NormalizedCost(
                new BigDecimal("1.00"), "USD", BigDecimal.ZERO, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
