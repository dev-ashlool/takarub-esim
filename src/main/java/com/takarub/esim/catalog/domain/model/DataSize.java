package com.takarub.esim.catalog.domain.model;

import com.takarub.esim.supplier.domain.model.DataUnit;

/**
 * Network data allowance expressed as a numeric value and bandwidth unit.
 */
public record DataSize(int value, DataUnit unit) {

    public DataSize {
        if (value < 1) {
            throw new IllegalArgumentException("value must be at least 1");
        }
        if (unit == null) {
            throw new IllegalArgumentException("unit must not be null");
        }
    }
}
