package com.takarub.esim.catalog.presentation.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin request to upsert an exchange rate used for supplier cost normalization.
 */
public record UpdateExchangeRateRequest(
        @NotBlank @Size(min = 3, max = 3) String baseCurrency,
        @NotBlank @Size(min = 3, max = 3) String targetCurrency,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal rate) {
}
