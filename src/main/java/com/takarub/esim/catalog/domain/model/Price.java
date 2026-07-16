package com.takarub.esim.catalog.domain.model;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Monetary amount paired with its ISO-4217 currency code.
 */
public record Price(BigDecimal amount, Currency currency) {

    public Price {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (currency == null) {
            throw new IllegalArgumentException("currency must not be null");
        }
    }
}
