package com.takarub.esim.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.catalog.domain.port.ExchangeRatePort;

/**
 * JPA-backed lookup and admin upsert of exchange rates.
 */
@Component
public class ExchangeRateAdapter implements ExchangeRatePort {

    private final ExchangeRateJpaRepository repository;

    public ExchangeRateAdapter(ExchangeRateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<BigDecimal> findRate(String baseCurrency, String targetCurrency) {
        if (baseCurrency == null || targetCurrency == null) {
            return Optional.empty();
        }
        String base = normalizeCurrency(baseCurrency);
        String target = normalizeCurrency(targetCurrency);
        return repository.findByBaseCurrencyAndTargetCurrency(base, target)
                .map(ExchangeRateEntity::getRate);
    }

    @Override
    public void upsertRate(String baseCurrency, String targetCurrency, BigDecimal rate) {
        String base = normalizeCurrency(baseCurrency);
        String target = normalizeCurrency(targetCurrency);
        Instant now = Instant.now();

        ExchangeRateEntity entity = repository.findByBaseCurrencyAndTargetCurrency(base, target)
                .orElseGet(() -> new ExchangeRateEntity(base, target, rate, now));

        entity.updateRate(rate, now);
        repository.save(entity);
    }

    private static String normalizeCurrency(String currency) {
        return currency.trim().toUpperCase(Locale.ROOT);
    }
}
