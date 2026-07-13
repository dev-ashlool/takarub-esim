package com.takarub.esim.pricing.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.model.PricingRuleType;
import com.takarub.esim.pricing.domain.model.PricingScope;
import com.takarub.esim.pricing.domain.port.PricingRulePort;

@Component
public class PricingRuleAdapter implements PricingRulePort {

    private final PricingRuleJpaRepository repository;

    public PricingRuleAdapter(PricingRuleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingRule> findEnabledGlobalPercentage() {
        return repository.findEnabledGlobal().map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingRule> findEnabledPackageRule(String catalogPackageId) {
        return repository.findByCatalogPackageIdAndEnabledTrue(catalogPackageId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingRule> findAllEnabledPackageRules() {
        return repository.findAllByScopeAndEnabledTrue(PricingScope.PACKAGE).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public PricingRule upsertGlobalPercentage(BigDecimal percentage) {
        Instant now = Instant.now();
        Optional<PricingRuleEntity> current = repository.findEnabledGlobal();
        if (current.isPresent()) {
            PricingRuleEntity existing = current.get();
            if (existing.getRuleType() == PricingRuleType.PERCENTAGE
                    && sameAmount(existing.getPercentage(), percentage)) {
                return toDomain(existing);
            }
            existing.disable(now);
            repository.save(existing);
        }
        return toDomain(repository.save(PricingRuleEntity.globalPercentage(percentage, now)));
    }

    @Override
    @Transactional
    public PricingRule upsertPackageRule(String catalogPackageId, PricingRuleType type,
                                         BigDecimal percentage, BigDecimal fixedPrice) {
        Instant now = Instant.now();
        Optional<PricingRuleEntity> current =
                repository.findByCatalogPackageIdAndEnabledTrue(catalogPackageId);
        if (current.isPresent()) {
            PricingRuleEntity existing = current.get();
            if (samePackageRule(existing, type, percentage, fixedPrice)) {
                return toDomain(existing);
            }
            existing.disable(now);
            repository.save(existing);
        }
        return toDomain(repository.save(PricingRuleEntity.packageRule(
                catalogPackageId, type, percentage, fixedPrice, now)));
    }

    @Override
    @Transactional
    public boolean deletePackageRule(String catalogPackageId) {
        Optional<PricingRuleEntity> existing =
                repository.findByCatalogPackageIdAndEnabledTrue(catalogPackageId);
        if (existing.isEmpty()) {
            return false;
        }
        PricingRuleEntity entity = existing.get();
        entity.disable(Instant.now());
        repository.save(entity);
        return true;
    }

    private static boolean samePackageRule(PricingRuleEntity existing, PricingRuleType type,
                                           BigDecimal percentage, BigDecimal fixedPrice) {
        if (existing.getRuleType() != type) {
            return false;
        }
        if (type == PricingRuleType.PERCENTAGE) {
            return sameAmount(existing.getPercentage(), percentage);
        }
        return sameAmount(existing.getFixedPrice(), fixedPrice);
    }

    private static boolean sameAmount(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return Objects.equals(left, right);
        }
        return left.compareTo(right) == 0;
    }

    private PricingRule toDomain(PricingRuleEntity entity) {
        return new PricingRule(
                entity.getId(),
                entity.getScope(),
                entity.getCatalogPackageId(),
                entity.getRuleType(),
                entity.getPercentage(),
                entity.getFixedPrice(),
                entity.getCurrency(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
