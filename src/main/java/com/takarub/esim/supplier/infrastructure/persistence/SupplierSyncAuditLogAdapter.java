package com.takarub.esim.supplier.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.takarub.esim.supplier.application.port.SupplierSyncAuditLogPort;
import com.takarub.esim.supplier.domain.model.SupplierSyncAuditLog;

@Component
public class SupplierSyncAuditLogAdapter implements SupplierSyncAuditLogPort {

    private final SupplierSyncAuditLogJpaRepository repository;

    public SupplierSyncAuditLogAdapter(SupplierSyncAuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SupplierSyncAuditLog save(SupplierSyncAuditLog auditLog) {
        SupplierSyncAuditLogEntity entity = toEntity(auditLog);
        SupplierSyncAuditLogEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierSyncAuditLog> findAll(String supplier, Instant from, Instant to, int page, int size) {
        Page<SupplierSyncAuditLogEntity> result = repository.findFiltered(
                supplier, from, to, PageRequest.of(page, size));
        return result.getContent().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count(String supplier, Instant from, Instant to) {
        return repository.findFiltered(supplier, from, to, PageRequest.of(0, 1)).getTotalElements();
    }

    private SupplierSyncAuditLogEntity toEntity(SupplierSyncAuditLog log) {
        SupplierSyncAuditLogEntity entity = new SupplierSyncAuditLogEntity();
        entity.setId(log.getId());
        entity.setSupplier(log.getSupplier());
        entity.setStatus(log.getStatus());
        entity.setStartedAt(log.getStartedAt());
        entity.setFinishedAt(log.getFinishedAt());
        entity.setDurationMs(log.getDurationMs());
        entity.setTotalProcessed(log.getTotalProcessed());
        entity.setCreatedCount(log.getCreatedCount());
        entity.setUpdatedCount(log.getUpdatedCount());
        entity.setFailedCount(log.getFailedCount());
        entity.setErrorMessage(log.getErrorMessage());
        entity.setSkippedRegionsCount(log.getSkippedRegionsCount());
        entity.setInvalidLocationCount(log.getInvalidLocationCount());
        entity.setRegionsProcessedCount(log.getRegionsProcessedCount());
        return entity;
    }

    private SupplierSyncAuditLog toDomain(SupplierSyncAuditLogEntity entity) {
        return new SupplierSyncAuditLog(
                entity.getId(),
                entity.getSupplier(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getDurationMs(),
                entity.getTotalProcessed(),
                entity.getCreatedCount(),
                entity.getUpdatedCount(),
                entity.getFailedCount(),
                entity.getErrorMessage(),
                entity.getSkippedRegionsCount(),
                entity.getInvalidLocationCount(),
                entity.getRegionsProcessedCount());
    }
}
