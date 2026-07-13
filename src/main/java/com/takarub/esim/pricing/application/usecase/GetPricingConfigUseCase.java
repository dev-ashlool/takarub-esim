package com.takarub.esim.pricing.application.usecase;

import com.takarub.esim.pricing.application.result.PricingConfigView;
import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.port.PricingRulePort;

public class GetPricingConfigUseCase {

    private final PricingRulePort pricingRulePort;

    public GetPricingConfigUseCase(PricingRulePort pricingRulePort) {
        this.pricingRulePort = pricingRulePort;
    }

    public PricingConfigView execute() {
        PricingConfigView.GlobalMarkupView global = pricingRulePort.findEnabledGlobalPercentage()
                .map(rule -> new PricingConfigView.GlobalMarkupView(rule.percentage(), rule.updatedAt()))
                .orElse(null);

        var packageRules = pricingRulePort.findAllEnabledPackageRules().stream()
                .map(this::toPackageView)
                .toList();

        return new PricingConfigView(global, packageRules);
    }

    private PricingConfigView.PackagePricingView toPackageView(PricingRule rule) {
        return new PricingConfigView.PackagePricingView(
                rule.catalogPackageId(),
                rule.type(),
                rule.percentage(),
                rule.fixedPrice(),
                rule.updatedAt());
    }
}
