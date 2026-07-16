package com.takarub.esim.pricing.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.takarub.esim.pricing.domain.model.PricingRuleType;

public record PricingConfigView(
        GlobalMarkupView globalMarkup,
        List<PackagePricingView> packageRules) {

    public record GlobalMarkupView(BigDecimal percentage, Instant updatedAt) {
    }

    public record PackagePricingView(
            String catalogPackageId,
            PricingRuleType type,
            BigDecimal percentage,
            BigDecimal fixedPrice,
            Instant updatedAt) {
    }
}
