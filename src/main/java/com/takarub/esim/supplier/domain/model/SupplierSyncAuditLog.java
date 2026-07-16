package com.takarub.esim.supplier.domain.model;

import java.time.Instant;

/**
 * Tracks a single supplier catalog sync execution from start to finish.
 */
public class SupplierSyncAuditLog {

    private Long id;
    private final String supplier;
    private SupplierSyncAuditStatus status;
    private final Instant startedAt;
    private Instant finishedAt;
    private Long durationMs;
    private int totalProcessed;
    private int createdCount;
    private int updatedCount;
    private int failedCount;
    private String errorMessage;
    private int skippedRegionsCount;
    private int invalidLocationCount;
    private int regionsProcessedCount;

    public SupplierSyncAuditLog(String supplier) {
        this.supplier = supplier;
        this.status = SupplierSyncAuditStatus.STARTED;
        this.startedAt = Instant.now();
    }

    public SupplierSyncAuditLog(Long id, String supplier, SupplierSyncAuditStatus status,
                                 Instant startedAt, Instant finishedAt, Long durationMs,
                                 int totalProcessed, int createdCount, int updatedCount,
                                 int failedCount, String errorMessage,
                                 int skippedRegionsCount, int invalidLocationCount, int regionsProcessedCount) {
        this.id = id;
        this.supplier = supplier;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.durationMs = durationMs;
        this.totalProcessed = totalProcessed;
        this.createdCount = createdCount;
        this.updatedCount = updatedCount;
        this.failedCount = failedCount;
        this.errorMessage = errorMessage;
        this.skippedRegionsCount = skippedRegionsCount;
        this.invalidLocationCount = invalidLocationCount;
        this.regionsProcessedCount = regionsProcessedCount;
    }

    public void markSuccess(int totalProcessed, int createdCount, int updatedCount, int failedCount,
                            int skippedRegionsCount, int invalidLocationCount, int regionsProcessedCount) {
        this.finishedAt = Instant.now();
        this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();
        this.totalProcessed = totalProcessed;
        this.createdCount = createdCount;
        this.updatedCount = updatedCount;
        this.failedCount = failedCount;
        this.skippedRegionsCount = skippedRegionsCount;
        this.invalidLocationCount = invalidLocationCount;
        this.regionsProcessedCount = regionsProcessedCount;
        this.status = failedCount > 0 ? SupplierSyncAuditStatus.PARTIAL : SupplierSyncAuditStatus.SUCCESS;
    }

    public void markFailed(String errorMessage) {
        this.finishedAt = Instant.now();
        this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();
        this.status = SupplierSyncAuditStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public String getSupplier() { return supplier; }
    public SupplierSyncAuditStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public int getTotalProcessed() { return totalProcessed; }
    public int getCreatedCount() { return createdCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getFailedCount() { return failedCount; }
    public String getErrorMessage() { return errorMessage; }
    public int getSkippedRegionsCount() { return skippedRegionsCount; }
    public int getInvalidLocationCount() { return invalidLocationCount; }
    public int getRegionsProcessedCount() { return regionsProcessedCount; }
}
