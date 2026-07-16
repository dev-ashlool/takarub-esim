package com.takarub.esim.pricing.application.command;

import java.math.BigDecimal;

import com.takarub.esim.pricing.domain.model.PricingRuleType;

public record UpsertPackagePricingCommand(
        String catalogPackageId,
        PricingRuleType type,
        BigDecimal percentage,
        BigDecimal fixedPrice) {
}
