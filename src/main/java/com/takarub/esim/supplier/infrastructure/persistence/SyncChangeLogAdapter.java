package com.takarub.esim.supplier.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.takarub.esim.supplier.application.port.SyncChangeLogPort;
import com.takarub.esim.supplier.domain.model.SyncChangeLog;

@Component
public class SyncChangeLogAdapter implements SyncChangeLogPort {

    private final SyncChangeLogJpaRepository repository;

    public SyncChangeLogAdapter(SyncChangeLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void saveAll(List<SyncChangeLog> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        List<SyncChangeLogEntity> entities = changes.stream()
                .map(this::toEntity)
                .toList();
        repository.saveAll(entities);
    }

    private SyncChangeLogEntity toEntity(SyncChangeLog change) {
        SyncChangeLogEntity entity = new SyncChangeLogEntity();
        entity.setSyncAuditLogId(change.getSyncAuditLogId());
        entity.setSupplier(change.getSupplier());
        entity.setRemoteProductId(change.getRemoteProductId());
        entity.setCatalogPackageId(change.getCatalogPackageId());
        entity.setChangeType(change.getChangeType());
        entity.setOldCostPrice(change.getOldCostPrice());
        entity.setNewCostPrice(change.getNewCostPrice());
        entity.setOldCostCurrency(change.getOldCostCurrency());
        entity.setNewCostCurrency(change.getNewCostCurrency());
        entity.setOldNormalizedCost(change.getOldNormalizedCost());
        entity.setNewNormalizedCost(change.getNewNormalizedCost());
        entity.setOldNormalizedCurrency(change.getOldNormalizedCurrency());
        entity.setNewNormalizedCurrency(change.getNewNormalizedCurrency());
        entity.setOldInStock(change.getOldInStock());
        entity.setNewInStock(change.getNewInStock());
        entity.setChangedAt(change.getChangedAt());
        return entity;
    }
}
