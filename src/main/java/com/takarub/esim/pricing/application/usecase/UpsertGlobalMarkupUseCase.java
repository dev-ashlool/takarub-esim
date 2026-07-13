package com.takarub.esim.pricing.application.usecase;

import java.math.BigDecimal;

import com.takarub.esim.catalog.infrastructure.cache.CatalogCacheInvalidator;
import com.takarub.esim.pricing.application.command.UpsertGlobalMarkupCommand;
import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.port.PricingRulePort;

public class UpsertGlobalMarkupUseCase {

    private final PricingRulePort pricingRulePort;
    private final CatalogCacheInvalidator cacheInvalidator;

    public UpsertGlobalMarkupUseCase(PricingRulePort pricingRulePort,
                                     CatalogCacheInvalidator cacheInvalidator) {
        this.pricingRulePort = pricingRulePort;
        this.cacheInvalidator = cacheInvalidator;
    }

    public PricingRule execute(UpsertGlobalMarkupCommand command) {
        if (command.percentage() == null || command.percentage().signum() < 0) {
            throw new IllegalArgumentException("percentage must be zero or greater");
        }
        PricingRule saved = pricingRulePort.upsertGlobalPercentage(command.percentage());
        cacheInvalidator.invalidateAll();
        return saved;
    }
}
