package com.takarub.esim.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persisted FX rate used for supplier cost normalization to USD.
 */
@Entity
@Table(name = "exchange_rates")
public class ExchangeRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "base_currency", length = 3, nullable = false)
    private String baseCurrency;

    @Column(name = "target_currency", length = 3, nullable = false)
    private String targetCurrency;

    @Column(name = "rate", precision = 18, scale = 8, nullable = false)
    private BigDecimal rate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExchangeRateEntity() {
        // Required by JPA.
    }

    public ExchangeRateEntity(String baseCurrency, String targetCurrency, BigDecimal rate, Instant updatedAt) {
        this.baseCurrency = baseCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateRate(BigDecimal rate, Instant updatedAt) {
        this.rate = rate;
        this.updatedAt = updatedAt;
    }
}
