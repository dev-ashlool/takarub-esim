package com.takarub.esim.pricing.application.usecase;

import java.math.BigDecimal;

import com.takarub.esim.catalog.infrastructure.cache.CatalogCacheInvalidator;
import com.takarub.esim.pricing.application.command.UpsertPackagePricingCommand;
import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.model.PricingRuleType;
import com.takarub.esim.pricing.domain.port.PricingRulePort;

public class UpsertPackagePricingUseCase {

    private final PricingRulePort pricingRulePort;
    private final CatalogCacheInvalidator cacheInvalidator;

    public UpsertPackagePricingUseCase(PricingRulePort pricingRulePort,
                                       CatalogCacheInvalidator cacheInvalidator) {
        this.pricingRulePort = pricingRulePort;
        this.cacheInvalidator = cacheInvalidator;
    }

    public PricingRule execute(UpsertPackagePricingCommand command) {
        if (command.catalogPackageId() == null || command.catalogPackageId().isBlank()) {
            throw new IllegalArgumentException("catalogPackageId must not be blank");
        }
        if (command.type() == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        BigDecimal percentage = null;
        BigDecimal fixedPrice = null;
        if (command.type() == PricingRuleType.PERCENTAGE) {
            if (command.percentage() == null || command.percentage().signum() < 0) {
                throw new IllegalArgumentException("percentage must be zero or greater for PERCENTAGE rules");
            }
            percentage = command.percentage();
        } else if (command.type() == PricingRuleType.FIXED) {
            if (command.fixedPrice() == null || command.fixedPrice().signum() <= 0) {
                throw new IllegalArgumentException("fixedPrice must be greater than zero for FIXED rules");
            }
            fixedPrice = command.fixedPrice();
        } else {
            throw new IllegalArgumentException("Unsupported pricing rule type: " + command.type());
        }

        PricingRule saved = pricingRulePort.upsertPackageRule(
                command.catalogPackageId().trim(), command.type(), percentage, fixedPrice);
        cacheInvalidator.invalidateAll();
        return saved;
    }
}
