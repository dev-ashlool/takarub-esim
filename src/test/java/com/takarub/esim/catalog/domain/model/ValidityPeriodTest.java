package com.takarub.esim.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidityPeriodTest {

    @Test
    void acceptsPositiveDayCount() {
        ValidityPeriod validityPeriod = new ValidityPeriod(30);

        assertThat(validityPeriod.days()).isEqualTo(30);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidDayCount(int days) {
        assertThatThrownBy(() -> new ValidityPeriod(days))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("days must be at least 1");
    }
}
