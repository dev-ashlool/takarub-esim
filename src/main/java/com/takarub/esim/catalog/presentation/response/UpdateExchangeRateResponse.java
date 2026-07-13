package com.takarub.esim.catalog.presentation.response;

import java.math.BigDecimal;

/**
 * Admin response after updating an exchange rate and recalculating normalized costs.
 */
public record UpdateExchangeRateResponse(
        String baseCurrency,
        String targetCurrency,
        BigDecimal rate,
        int mappingsRecalculated) {
}
