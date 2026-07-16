package com.takarub.esim.supplier.application.port;

import java.time.Instant;
import java.util.List;

import com.takarub.esim.supplier.domain.model.SupplierSyncAuditLog;

/**
 * Persistence port for supplier sync audit logs.
 */
public interface SupplierSyncAuditLogPort {

    SupplierSyncAuditLog save(SupplierSyncAuditLog auditLog);

    List<SupplierSyncAuditLog> findAll(String supplier, Instant from, Instant to, int page, int size);

    long count(String supplier, Instant from, Instant to);
}
