package com.takarub.esim.catalog.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import com.takarub.esim.catalog.domain.exceptions.CurrencyExchangeException;
import com.takarub.esim.catalog.domain.model.NormalizedCost;
import com.takarub.esim.catalog.domain.port.ExchangeRatePort;

/**
 * Converts supplier costs into the platform base currency (USD) for multi-supplier comparison.
 * USD inputs are identity (no rate lookup). Non-USD inputs require a DB rate.
 */
public class CurrencyNormalizationService {

    public static final String PLATFORM_CURRENCY = "USD";
    private static final int NORMALIZED_SCALE = 4;

    private final ExchangeRatePort exchangeRatePort;

    public CurrencyNormalizationService(ExchangeRatePort exchangeRatePort) {
        this.exchangeRatePort = exchangeRatePort;
    }

    public NormalizedCost normalize(BigDecimal amount, String currency) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        String originalCurrency = currency.trim().toUpperCase(Locale.ROOT);

        if (PLATFORM_CURRENCY.equals(originalCurrency)) {
            return new NormalizedCost(amount, originalCurrency, amount, PLATFORM_CURRENCY);
        }

        BigDecimal rate = exchangeRatePort.findRate(originalCurrency, PLATFORM_CURRENCY)
                .orElseThrow(() -> new CurrencyExchangeException(
                        "No exchange rate found for " + originalCurrency + " -> " + PLATFORM_CURRENCY));

        return new NormalizedCost(amount, originalCurrency, applyRate(amount, rate), PLATFORM_CURRENCY);
    }

    /**
     * Applies an already-known FX rate using the same scale/rounding as {@link #normalize}.
     */
    public BigDecimal applyRate(BigDecimal amount, BigDecimal rate) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be greater than zero");
        }
        return amount.multiply(rate).setScale(NORMALIZED_SCALE, RoundingMode.HALF_UP);
    }
}
