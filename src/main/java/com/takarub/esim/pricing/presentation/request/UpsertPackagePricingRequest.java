package com.takarub.esim.pricing.presentation.request;

import java.math.BigDecimal;

import com.takarub.esim.pricing.domain.model.PricingRuleType;

import jakarta.validation.constraints.NotNull;

public record UpsertPackagePricingRequest(
        @NotNull PricingRuleType type,
        BigDecimal percentage,
        BigDecimal fixedPrice) {
}
