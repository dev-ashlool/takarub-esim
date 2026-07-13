package com.takarub.esim.pricing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.catalog.infrastructure.cache.CatalogCacheInvalidator;
import com.takarub.esim.pricing.application.usecase.DeletePackagePricingUseCase;
import com.takarub.esim.pricing.application.usecase.GetPricingConfigUseCase;
import com.takarub.esim.pricing.application.usecase.UpsertGlobalMarkupUseCase;
import com.takarub.esim.pricing.application.usecase.UpsertPackagePricingUseCase;
import com.takarub.esim.pricing.domain.port.CheapestNormalizedCostPort;
import com.takarub.esim.pricing.domain.port.PricingRulePort;
import com.takarub.esim.pricing.domain.service.SellPriceResolver;

@Configuration
public class PricingUseCaseConfig {

    @Bean
    public SellPriceResolver sellPriceResolver(CheapestNormalizedCostPort cheapestNormalizedCostPort,
                                               PricingRulePort pricingRulePort) {
        return new SellPriceResolver(cheapestNormalizedCostPort, pricingRulePort);
    }

    @Bean
    public UpsertGlobalMarkupUseCase upsertGlobalMarkupUseCase(PricingRulePort pricingRulePort,
                                                               CatalogCacheInvalidator cacheInvalidator) {
        return new UpsertGlobalMarkupUseCase(pricingRulePort, cacheInvalidator);
    }

    @Bean
    public UpsertPackagePricingUseCase upsertPackagePricingUseCase(PricingRulePort pricingRulePort,
                                                                   CatalogCacheInvalidator cacheInvalidator) {
        return new UpsertPackagePricingUseCase(pricingRulePort, cacheInvalidator);
    }

    @Bean
    public DeletePackagePricingUseCase deletePackagePricingUseCase(PricingRulePort pricingRulePort,
                                                                   CatalogCacheInvalidator cacheInvalidator) {
        return new DeletePackagePricingUseCase(pricingRulePort, cacheInvalidator);
    }

    @Bean
    public GetPricingConfigUseCase getPricingConfigUseCase(PricingRulePort pricingRulePort) {
        return new GetPricingConfigUseCase(pricingRulePort);
    }
}
