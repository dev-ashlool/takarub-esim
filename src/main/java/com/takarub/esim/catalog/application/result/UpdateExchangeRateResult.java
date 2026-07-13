package com.takarub.esim.catalog.application.result;

import java.math.BigDecimal;

/**
 * Result of an admin exchange-rate update and optional normalized-cost recalculation.
 */
public record UpdateExchangeRateResult(
        String baseCurrency,
        String targetCurrency,
        BigDecimal rate,
        int mappingsRecalculated) {
}
