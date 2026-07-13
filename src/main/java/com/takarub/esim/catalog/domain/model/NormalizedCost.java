package com.takarub.esim.catalog.domain.model;

import java.math.BigDecimal;

/**
 * Supplier cost before and after normalization to the platform base currency (USD).
 */
public record NormalizedCost(
        BigDecimal originalAmount,
        String originalCurrency,
        BigDecimal normalizedAmount,
        String normalizedCurrency) {

    public NormalizedCost {
        if (originalAmount == null || originalAmount.signum() <= 0) {
            throw new IllegalArgumentException("originalAmount must be greater than zero");
        }
        if (originalCurrency == null || originalCurrency.isBlank()) {
            throw new IllegalArgumentException("originalCurrency must not be blank");
        }
        if (normalizedAmount == null || normalizedAmount.signum() <= 0) {
            throw new IllegalArgumentException("normalizedAmount must be greater than zero");
        }
        if (normalizedCurrency == null || normalizedCurrency.isBlank()) {
            throw new IllegalArgumentException("normalizedCurrency must not be blank");
        }
    }
}
