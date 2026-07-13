package com.takarub.esim.pricing.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.model.PricingRuleType;
import com.takarub.esim.pricing.domain.model.SellPrice;
import com.takarub.esim.pricing.domain.port.CheapestNormalizedCostPort;
import com.takarub.esim.pricing.domain.port.PricingRulePort;

/**
 * Resolves end-user sell price from cheapest normalized cost + pricing rule precedence.
 */
public class SellPriceResolver {

    /** Customer-facing money scale (USD). */
    public static final int SELL_PRICE_SCALE = 2;

    private final CheapestNormalizedCostPort cheapestNormalizedCostPort;
    private final PricingRulePort pricingRulePort;

    public SellPriceResolver(CheapestNormalizedCostPort cheapestNormalizedCostPort,
                             PricingRulePort pricingRulePort) {
        this.cheapestNormalizedCostPort = cheapestNormalizedCostPort;
        this.pricingRulePort = pricingRulePort;
    }

    public Optional<SellPrice> resolve(String catalogPackageId) {
        if (catalogPackageId == null || catalogPackageId.isBlank()) {
            return Optional.empty();
        }

        Optional<PricingRule> packageRule = pricingRulePort.findEnabledPackageRule(catalogPackageId);
        if (packageRule.isPresent() && packageRule.get().type() == PricingRuleType.FIXED) {
            return Optional.of(SellPrice.usd(scale(packageRule.get().fixedPrice())));
        }

        Optional<BigDecimal> cheapestCost = cheapestNormalizedCostPort
                .findCheapestInStockNormalizedCostUsd(catalogPackageId);
        if (cheapestCost.isEmpty()) {
            return Optional.empty();
        }

        if (packageRule.isPresent() && packageRule.get().type() == PricingRuleType.PERCENTAGE) {
            return Optional.of(SellPrice.usd(applyPercentage(cheapestCost.get(), packageRule.get().percentage())));
        }

        Optional<PricingRule> global = pricingRulePort.findEnabledGlobalPercentage();
        if (global.isPresent()) {
            return Optional.of(SellPrice.usd(applyPercentage(cheapestCost.get(), global.get().percentage())));
        }

        return Optional.empty();
    }

    static BigDecimal applyPercentage(BigDecimal cost, BigDecimal percentage) {
        BigDecimal multiplier = BigDecimal.ONE.add(
                percentage.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        return scale(cost.multiply(multiplier));
    }

    static BigDecimal scale(BigDecimal amount) {
        return amount.setScale(SELL_PRICE_SCALE, RoundingMode.HALF_UP);
    }
}
