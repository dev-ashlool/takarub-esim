package com.takarub.esim.catalog.domain.model;

/**
 * Plan validity window measured in whole days.
 */
public record ValidityPeriod(int days) {

    public ValidityPeriod {
        if (days < 1) {
            throw new IllegalArgumentException("days must be at least 1");
        }
    }
}
