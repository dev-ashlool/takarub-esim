package com.takarub.esim.pricing.presentation.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpsertGlobalMarkupRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal percentage) {
}
