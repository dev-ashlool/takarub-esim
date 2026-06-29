package com.takarub.esim.supplier.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierPackageMappingJpaRepository extends JpaRepository<SupplierPackageMappingEntity, Integer> {

    Optional<SupplierPackageMappingEntity> findBySupplierKeyAndRemoteProductId(
            String supplierKey, String remoteProductId);

    List<SupplierPackageMappingEntity> findAllBySupplierKey(String supplierKey);
}
