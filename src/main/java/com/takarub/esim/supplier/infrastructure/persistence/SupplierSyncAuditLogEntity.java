package com.takarub.esim.supplier.infrastructure.persistence;

import java.time.Instant;

import com.takarub.esim.supplier.domain.model.SupplierSyncAuditStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "supplier_sync_audit_logs")
public class SupplierSyncAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier", length = 50, nullable = false)
    private String supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SupplierSyncAuditStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "total_processed", nullable = false)
    private int totalProcessed;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "skipped_regions_count", nullable = false)
    private int skippedRegionsCount;

    @Column(name = "invalid_location_count", nullable = false)
    private int invalidLocationCount;

    @Column(name = "regions_processed_count", nullable = false)
    private int regionsProcessedCount;

    protected SupplierSyncAuditLogEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public SupplierSyncAuditStatus getStatus() { return status; }
    public void setStatus(SupplierSyncAuditStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }
    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getSkippedRegionsCount() { return skippedRegionsCount; }
    public void setSkippedRegionsCount(int skippedRegionsCount) { this.skippedRegionsCount = skippedRegionsCount; }
    public int getInvalidLocationCount() { return invalidLocationCount; }
    public void setInvalidLocationCount(int invalidLocationCount) { this.invalidLocationCount = invalidLocationCount; }
    public int getRegionsProcessedCount() { return regionsProcessedCount; }
    public void setRegionsProcessedCount(int regionsProcessedCount) { this.regionsProcessedCount = regionsProcessedCount; }
}
