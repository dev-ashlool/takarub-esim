package com.takarub.esim.catalog.domain.port;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Reads and writes FX conversion rates (seeded / admin-maintained).
 */
public interface ExchangeRatePort {

    /**
     * @return rate such that {@code amountInBase * rate = amountInTarget}, if present
     */
    Optional<BigDecimal> findRate(String baseCurrency, String targetCurrency);

    /**
     * Inserts or updates the rate for a currency pair and refreshes {@code updated_at}.
     */
    void upsertRate(String baseCurrency, String targetCurrency, BigDecimal rate);
}
