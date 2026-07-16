package com.takarub.esim.supplier.application.port;

import java.util.List;

import com.takarub.esim.supplier.domain.model.SyncChangeLog;

/**
 * Persists package-level deltas produced during a supplier sync run.
 */
public interface SyncChangeLogPort {

    void saveAll(List<SyncChangeLog> changes);
}
