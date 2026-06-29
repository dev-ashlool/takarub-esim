package com.takarub.esim.supplier.domain.model;

import java.math.BigDecimal;

/**
 * Immutable unified representation of a remote supplier catalog item before local normalization.
 */
public record RawSupplierProduct(
        String id,
        String countryIso,
        BigDecimal costPrice,
        String costCurrency,
        int dataAmount,
        DataUnit dataUnit,
        int durationDays) {

    public RawSupplierProduct {
        if (costPrice == null || costPrice.signum() <= 0) {
            throw new IllegalArgumentException("costPrice must be greater than zero");
        }
        if (dataAmount < 1) {
            throw new IllegalArgumentException("dataAmount must be at least 1");
        }
        if (durationDays < 1) {
            throw new IllegalArgumentException("durationDays must be at least 1");
        }
    }
}
