package com.takarub.esim.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.takarub.esim.supplier.domain.model.DataUnit;

class DataSizeTest {

    @Test
    void acceptsValidValueAndUnit() {
        DataSize dataSize = new DataSize(20, DataUnit.GB);

        assertThat(dataSize.value()).isEqualTo(20);
        assertThat(dataSize.unit()).isEqualTo(DataUnit.GB);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidValue(int value) {
        assertThatThrownBy(() -> new DataSize(value, DataUnit.MB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be at least 1");
    }

    @Test
    void rejectsNullUnit() {
        assertThatThrownBy(() -> new DataSize(1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unit must not be null");
    }
}
