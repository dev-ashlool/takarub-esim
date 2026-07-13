package com.takarub.esim.pricing.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.infrastructure.cache.CatalogCacheInvalidator;
import com.takarub.esim.pricing.application.command.UpsertGlobalMarkupCommand;
import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.model.PricingRuleType;
import com.takarub.esim.pricing.domain.model.PricingScope;
import com.takarub.esim.pricing.domain.port.PricingRulePort;

@ExtendWith(MockitoExtension.class)
class UpsertGlobalMarkupUseCaseTest {

    @Mock
    private PricingRulePort pricingRulePort;

    @Mock
    private CatalogCacheInvalidator cacheInvalidator;

    private UpsertGlobalMarkupUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpsertGlobalMarkupUseCase(pricingRulePort, cacheInvalidator);
    }

    @Test
    void upsertsGlobalMarkupAndInvalidatesCatalogCache() {
        Instant now = Instant.now();
        PricingRule saved = new PricingRule(
                1, PricingScope.GLOBAL, null, PricingRuleType.PERCENTAGE,
                new BigDecimal("20"), null, "USD", true, now, now);
        when(pricingRulePort.upsertGlobalPercentage(new BigDecimal("20"))).thenReturn(saved);

        PricingRule result = useCase.execute(new UpsertGlobalMarkupCommand(new BigDecimal("20")));

        assertThat(result.percentage()).isEqualByComparingTo("20");
        verify(cacheInvalidator).invalidateAll();
    }
}
