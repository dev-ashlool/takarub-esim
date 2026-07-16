package com.takarub.esim.pricing.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Persisted pricing rule used to compute end-user sell price.
 * Historical versions remain stored with {@code enabled=false}.
 */
public record PricingRule(
        Integer id,
        PricingScope scope,
        String catalogPackageId,
        PricingRuleType type,
        BigDecimal percentage,
        BigDecimal fixedPrice,
        String currency,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {
}
