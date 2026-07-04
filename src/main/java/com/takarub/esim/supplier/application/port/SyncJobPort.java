package com.takarub.esim.supplier.application.port;

import java.util.List;
import java.util.Optional;

import com.takarub.esim.supplier.domain.model.SyncJob;

public interface SyncJobPort {

    List<SyncJob> findAll();

    List<SyncJob> findEnabled();

    Optional<SyncJob> findBySupplierName(String supplierName);

    Optional<SyncJob> findById(Long id);

    SyncJob save(SyncJob syncJob);

    void delete(Long id);
}
