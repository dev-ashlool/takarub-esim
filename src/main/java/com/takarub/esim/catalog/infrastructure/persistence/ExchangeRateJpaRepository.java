package com.takarub.esim.catalog.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateEntity, Integer> {

    Optional<ExchangeRateEntity> findByBaseCurrencyAndTargetCurrency(String baseCurrency, String targetCurrency);
}
