package com.takarub.esim.pricing.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.takarub.esim.pricing.domain.model.PricingScope;

public interface PricingRuleJpaRepository extends JpaRepository<PricingRuleEntity, Integer> {

    @Query("""
            SELECT r FROM PricingRuleEntity r
            WHERE r.scope = com.takarub.esim.pricing.domain.model.PricingScope.GLOBAL
              AND r.enabled = true
            """)
    Optional<PricingRuleEntity> findEnabledGlobal();

    Optional<PricingRuleEntity> findByCatalogPackageIdAndEnabledTrue(String catalogPackageId);

    List<PricingRuleEntity> findAllByCatalogPackageIdOrderByIdAsc(String catalogPackageId);

    List<PricingRuleEntity> findAllByScopeAndEnabledTrue(PricingScope scope);
}
