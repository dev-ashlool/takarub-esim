package com.takarub.esim.pricing.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

import com.takarub.esim.pricing.domain.model.PricingRuleType;
import com.takarub.esim.pricing.domain.model.PricingScope;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pricing_rules")
public class PricingRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 10, nullable = false)
    private PricingScope scope;

    @Column(name = "catalog_package_id", length = 36)
    private String catalogPackageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", length = 20, nullable = false)
    private PricingRuleType ruleType;

    @Column(name = "percentage", precision = 8, scale = 4)
    private BigDecimal percentage;

    @Column(name = "fixed_price", precision = 12, scale = 4)
    private BigDecimal fixedPrice;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PricingRuleEntity() {
    }

    public static PricingRuleEntity globalPercentage(BigDecimal percentage, Instant at) {
        PricingRuleEntity entity = new PricingRuleEntity();
        entity.scope = PricingScope.GLOBAL;
        entity.catalogPackageId = null;
        entity.ruleType = PricingRuleType.PERCENTAGE;
        entity.percentage = percentage;
        entity.fixedPrice = null;
        entity.currency = "USD";
        entity.enabled = true;
        entity.createdAt = at;
        entity.updatedAt = at;
        return entity;
    }

    public static PricingRuleEntity packageRule(String catalogPackageId, PricingRuleType type,
                                                BigDecimal percentage, BigDecimal fixedPrice,
                                                Instant at) {
        PricingRuleEntity entity = new PricingRuleEntity();
        entity.scope = PricingScope.PACKAGE;
        entity.catalogPackageId = catalogPackageId;
        entity.ruleType = type;
        entity.percentage = percentage;
        entity.fixedPrice = fixedPrice;
        entity.currency = "USD";
        entity.enabled = true;
        entity.createdAt = at;
        entity.updatedAt = at;
        return entity;
    }

    public Integer getId() {
        return id;
    }

    public PricingScope getScope() {
        return scope;
    }

    public String getCatalogPackageId() {
        return catalogPackageId;
    }

    public PricingRuleType getRuleType() {
        return ruleType;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public BigDecimal getFixedPrice() {
        return fixedPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Marks this version inactive. {@code updatedAt} becomes the version end time.
     */
    public void disable(Instant at) {
        this.enabled = false;
        this.updatedAt = at;
    }
}
