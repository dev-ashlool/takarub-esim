package com.takarub.esim.supplier.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.takarub.esim.supplier.application.port.SyncJobPort;
import com.takarub.esim.supplier.domain.model.SyncJob;

@Component
public class SyncJobAdapter implements SyncJobPort {

    private final SyncJobJpaRepository repository;

    public SyncJobAdapter(SyncJobJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncJob> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncJob> findEnabled() {
        return repository.findByEnabledTrue().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SyncJob> findBySupplierName(String supplierName) {
        return repository.findBySupplierName(supplierName).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SyncJob> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public SyncJob save(SyncJob syncJob) {
        SyncJobEntity entity;
        if (syncJob.getId() != null) {
            entity = repository.findById(syncJob.getId()).orElse(new SyncJobEntity());
        } else {
            entity = new SyncJobEntity();
        }
        entity.setSupplierName(syncJob.getSupplierName());
        entity.setCronExpression(syncJob.getCronExpression());
        entity.setEnabled(syncJob.isEnabled());
        entity.setLastRunTime(syncJob.getLastRunTime());
        entity.setNextRunTime(syncJob.getNextRunTime());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.Instant.now());
        }
        entity.setUpdatedAt(java.time.Instant.now());
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private SyncJob toDomain(SyncJobEntity entity) {
        return new SyncJob(
                entity.getId(),
                entity.getSupplierName(),
                entity.getCronExpression(),
                entity.isEnabled(),
                entity.getLastRunTime(),
                entity.getNextRunTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
