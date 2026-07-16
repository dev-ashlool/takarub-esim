package com.takarub.esim.supplier.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncJobJpaRepository extends JpaRepository<SyncJobEntity, Long> {

    List<SyncJobEntity> findByEnabledTrue();

    Optional<SyncJobEntity> findBySupplierName(String supplierName);
}
