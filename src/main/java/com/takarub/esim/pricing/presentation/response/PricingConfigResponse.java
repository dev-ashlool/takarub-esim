package com.takarub.esim.pricing.presentation.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.takarub.esim.pricing.domain.model.PricingRuleType;

public record PricingConfigResponse(
        GlobalMarkupResponse globalMarkup,
        List<PackagePricingResponse> packageRules) {

    public record GlobalMarkupResponse(BigDecimal percentage, Instant updatedAt) {
    }

    public record PackagePricingResponse(
            String catalogPackageId,
            PricingRuleType type,
            BigDecimal percentage,
            BigDecimal fixedPrice,
            Instant updatedAt) {
    }
}
