package com.takarub.esim.supplier.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncChangeLogJpaRepository extends JpaRepository<SyncChangeLogEntity, Long> {

    List<SyncChangeLogEntity> findAllBySyncAuditLogIdOrderByIdAsc(Long syncAuditLogId);
}
