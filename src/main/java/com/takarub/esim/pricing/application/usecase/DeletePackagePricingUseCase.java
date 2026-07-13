package com.takarub.esim.pricing.application.usecase;

import com.takarub.esim.catalog.infrastructure.cache.CatalogCacheInvalidator;
import com.takarub.esim.pricing.domain.port.PricingRulePort;

public class DeletePackagePricingUseCase {

    private final PricingRulePort pricingRulePort;
    private final CatalogCacheInvalidator cacheInvalidator;

    public DeletePackagePricingUseCase(PricingRulePort pricingRulePort,
                                       CatalogCacheInvalidator cacheInvalidator) {
        this.pricingRulePort = pricingRulePort;
        this.cacheInvalidator = cacheInvalidator;
    }

    public boolean execute(String catalogPackageId) {
        if (catalogPackageId == null || catalogPackageId.isBlank()) {
            throw new IllegalArgumentException("catalogPackageId must not be blank");
        }
        boolean deleted = pricingRulePort.deletePackageRule(catalogPackageId.trim());
        if (deleted) {
            cacheInvalidator.invalidateAll();
        }
        return deleted;
    }
}
