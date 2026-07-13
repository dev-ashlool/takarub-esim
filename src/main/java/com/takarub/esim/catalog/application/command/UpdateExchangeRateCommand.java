package com.takarub.esim.catalog.application.command;

import java.math.BigDecimal;

/**
 * Admin command to upsert an FX rate and recalculate affected normalized supplier costs.
 */
public record UpdateExchangeRateCommand(
        String baseCurrency,
        String targetCurrency,
        BigDecimal rate) {
}
