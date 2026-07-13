package com.takarub.esim.pricing.presentation.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.takarub.esim.pricing.domain.model.PricingRuleType;
import com.takarub.esim.pricing.domain.model.PricingScope;

public record PricingRuleResponse(
        Integer id,
        PricingScope scope,
        String catalogPackageId,
        PricingRuleType type,
        BigDecimal percentage,
        BigDecimal fixedPrice,
        String currency,
        Instant updatedAt) {
}
