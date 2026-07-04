package com.takarub.esim.supplier.infrastructure.persistence;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierSyncAuditLogJpaRepository extends JpaRepository<SupplierSyncAuditLogEntity, Long> {

    @Query("""
            SELECT e FROM SupplierSyncAuditLogEntity e
            WHERE (:supplier IS NULL OR e.supplier = :supplier)
              AND (:from IS NULL OR e.startedAt >= :from)
              AND (:to IS NULL OR e.startedAt <= :to)
            ORDER BY e.startedAt DESC
            """)
    Page<SupplierSyncAuditLogEntity> findFiltered(
            @Param("supplier") String supplier,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
