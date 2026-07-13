package com.takarub.esim.pricing.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.model.PricingRuleType;
import com.takarub.esim.pricing.domain.model.PricingScope;
import com.takarub.esim.pricing.domain.model.SellPrice;
import com.takarub.esim.pricing.domain.port.CheapestNormalizedCostPort;
import com.takarub.esim.pricing.domain.port.PricingRulePort;

@ExtendWith(MockitoExtension.class)
class SellPriceResolverTest {

    @Mock
    private CheapestNormalizedCostPort cheapestNormalizedCostPort;

    @Mock
    private PricingRulePort pricingRulePort;

    private SellPriceResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SellPriceResolver(cheapestNormalizedCostPort, pricingRulePort);
    }

    @Test
    void usesPackageFixedWithoutLookingUpCost() {
        when(pricingRulePort.findEnabledPackageRule("pkg-1")).thenReturn(Optional.of(
                packageRule(PricingRuleType.FIXED, null, new BigDecimal("15.00"))));

        Optional<SellPrice> result = resolver.resolve("pkg-1");

        assertThat(result).isPresent();
        assertThat(result.get().amount()).isEqualByComparingTo("15.00");
        assertThat(result.get().currency()).isEqualTo("USD");
        verifyNoInteractions(cheapestNormalizedCostPort);
    }

    @Test
    void packagePercentageBeatsGlobal() {
        when(pricingRulePort.findEnabledPackageRule("pkg-1")).thenReturn(Optional.of(
                packageRule(PricingRuleType.PERCENTAGE, new BigDecimal("50"), null)));
        when(cheapestNormalizedCostPort.findCheapestInStockNormalizedCostUsd("pkg-1"))
                .thenReturn(Optional.of(new BigDecimal("10.00")));

        Optional<SellPrice> result = resolver.resolve("pkg-1");

        assertThat(result).isPresent();
        assertThat(result.get().amount()).isEqualByComparingTo("15.00");
    }

    @Test
    void fallsBackToGlobalPercentage() {
        when(pricingRulePort.findEnabledPackageRule("pkg-1")).thenReturn(Optional.empty());
        when(cheapestNormalizedCostPort.findCheapestInStockNormalizedCostUsd("pkg-1"))
                .thenReturn(Optional.of(new BigDecimal("10.00")));
        when(pricingRulePort.findEnabledGlobalPercentage()).thenReturn(Optional.of(
                globalRule(new BigDecimal("20"))));

        Optional<SellPrice> result = resolver.resolve("pkg-1");

        assertThat(result).isPresent();
        assertThat(result.get().amount()).isEqualByComparingTo("12.00");
    }

    @Test
    void returnsEmptyWhenNoRule() {
        when(pricingRulePort.findEnabledPackageRule("pkg-1")).thenReturn(Optional.empty());
        when(cheapestNormalizedCostPort.findCheapestInStockNormalizedCostUsd("pkg-1"))
                .thenReturn(Optional.of(new BigDecimal("10.00")));
        when(pricingRulePort.findEnabledGlobalPercentage()).thenReturn(Optional.empty());

        assertThat(resolver.resolve("pkg-1")).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoInStockNormalizedCostForPercentageRules() {
        when(pricingRulePort.findEnabledPackageRule("pkg-1")).thenReturn(Optional.empty());
        when(cheapestNormalizedCostPort.findCheapestInStockNormalizedCostUsd("pkg-1"))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolve("pkg-1")).isEmpty();
        verify(cheapestNormalizedCostPort).findCheapestInStockNormalizedCostUsd("pkg-1");
    }

    private static PricingRule packageRule(PricingRuleType type, BigDecimal percentage, BigDecimal fixed) {
        Instant now = Instant.now();
        return new PricingRule(1, PricingScope.PACKAGE, "pkg-1", type, percentage, fixed, "USD", true, now, now);
    }

    private static PricingRule globalRule(BigDecimal percentage) {
        Instant now = Instant.now();
        return new PricingRule(2, PricingScope.GLOBAL, null, PricingRuleType.PERCENTAGE,
                percentage, null, "USD", true, now, now);
    }
}
