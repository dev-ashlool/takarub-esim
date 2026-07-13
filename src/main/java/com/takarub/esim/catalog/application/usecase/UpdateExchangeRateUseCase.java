package com.takarub.esim.catalog.application.usecase;

import java.math.BigDecimal;
import java.util.Locale;

import com.takarub.esim.catalog.application.command.UpdateExchangeRateCommand;
import com.takarub.esim.catalog.application.result.UpdateExchangeRateResult;
import com.takarub.esim.catalog.domain.port.ExchangeRatePort;
import com.takarub.esim.catalog.domain.service.CurrencyNormalizationService;
import com.takarub.esim.supplier.application.port.SupplierPackageMappingPort;

/**
 * Upserts an exchange rate, then recalculates normalized costs for non-USD original mappings.
 * Does not re-sync supplier catalogs and never modifies original cost fields.
 */
public class UpdateExchangeRateUseCase {

    private final ExchangeRatePort exchangeRatePort;
    private final SupplierPackageMappingPort packageMappingPort;

    public UpdateExchangeRateUseCase(ExchangeRatePort exchangeRatePort,
                                     SupplierPackageMappingPort packageMappingPort) {
        this.exchangeRatePort = exchangeRatePort;
        this.packageMappingPort = packageMappingPort;
    }

    public UpdateExchangeRateResult execute(UpdateExchangeRateCommand command) {
        validate(command);

        String base = command.baseCurrency().trim().toUpperCase(Locale.ROOT);
        String target = command.targetCurrency().trim().toUpperCase(Locale.ROOT);
        BigDecimal rate = command.rate();

        exchangeRatePort.upsertRate(base, target, rate);

        int recalculated = 0;
        if (!CurrencyNormalizationService.PLATFORM_CURRENCY.equals(base)) {
            recalculated = packageMappingPort.recalculateNormalizedCosts(base, rate);
        }

        return new UpdateExchangeRateResult(base, target, rate, recalculated);
    }

    private static void validate(UpdateExchangeRateCommand command) {
        if (command.baseCurrency() == null || command.baseCurrency().isBlank()) {
            throw new IllegalArgumentException("baseCurrency must not be blank");
        }
        if (command.targetCurrency() == null || command.targetCurrency().isBlank()) {
            throw new IllegalArgumentException("targetCurrency must not be blank");
        }
        if (command.rate() == null || command.rate().signum() <= 0) {
            throw new IllegalArgumentException("rate must be greater than zero");
        }

        String base = command.baseCurrency().trim();
        String target = command.targetCurrency().trim();
        if (base.length() != 3 || target.length() != 3) {
            throw new IllegalArgumentException("currency codes must be 3 letters (ISO-4217)");
        }

        if (!CurrencyNormalizationService.PLATFORM_CURRENCY.equalsIgnoreCase(target)) {
            throw new IllegalArgumentException("targetCurrency must be USD");
        }
    }
}
