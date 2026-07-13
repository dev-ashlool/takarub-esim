package com.takarub.esim.pricing.domain.model;

import java.math.BigDecimal;

/**
 * End-user sell price. Always USD for TASK-026.
 */
public record SellPrice(BigDecimal amount, String currency) {

    public static final String USD = "USD";

    public SellPrice {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("sell price amount must be greater than zero");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("sell price currency must not be blank");
        }
    }

    public static SellPrice usd(BigDecimal amount) {
        return new SellPrice(amount, USD);
    }
}
