package com.takarub.esim.pricing.domain.port;

import java.util.List;
import java.util.Optional;

import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.model.PricingRuleType;

/**
 * Persistence port for pricing rules.
 */
public interface PricingRulePort {

    Optional<PricingRule> findEnabledGlobalPercentage();

    Optional<PricingRule> findEnabledPackageRule(String catalogPackageId);

    List<PricingRule> findAllEnabledPackageRules();

    PricingRule upsertGlobalPercentage(java.math.BigDecimal percentage);

    PricingRule upsertPackageRule(String catalogPackageId, PricingRuleType type,
                                  java.math.BigDecimal percentage, java.math.BigDecimal fixedPrice);

    boolean deletePackageRule(String catalogPackageId);
}
