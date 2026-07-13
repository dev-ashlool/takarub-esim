package com.takarub.esim.supplier.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierPackageMappingJpaRepository extends JpaRepository<SupplierPackageMappingEntity, Integer> {

    Optional<SupplierPackageMappingEntity> findBySupplierKeyAndRemoteProductId(
            String supplierKey, String remoteProductId);

    List<SupplierPackageMappingEntity> findAllBySupplierKey(String supplierKey);

    @Query("SELECT m FROM SupplierPackageMappingEntity m WHERE UPPER(m.costCurrency) = UPPER(:currency)")
    List<SupplierPackageMappingEntity> findAllByCostCurrencyIgnoreCase(@Param("currency") String currency);

    @Query("""
            SELECT MIN(m.normalizedCostPrice)
            FROM SupplierPackageMappingEntity m
            WHERE m.catalogPackage.id = :packageId
              AND m.inStock = true
            """)
    Optional<BigDecimal> findMinNormalizedCostForInStockPackage(@Param("packageId") String packageId);
}
