package com.takarub.esim.supplier.domain.model;

import java.time.Instant;

/**
 * Represents a scheduled supplier sync job driven by a database-stored cron expression.
 */
public class SyncJob {

    private Long id;
    private final String supplierName;
    private String cronExpression;
    private boolean enabled;
    private Instant lastRunTime;
    private Instant nextRunTime;
    private Instant createdAt;
    private Instant updatedAt;

    public SyncJob(String supplierName, String cronExpression, boolean enabled) {
        this.supplierName = supplierName;
        this.cronExpression = cronExpression;
        this.enabled = enabled;
    }

    public SyncJob(Long id, String supplierName, String cronExpression, boolean enabled,
                   Instant lastRunTime, Instant nextRunTime, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.supplierName = supplierName;
        this.cronExpression = cronExpression;
        this.enabled = enabled;
        this.lastRunTime = lastRunTime;
        this.nextRunTime = nextRunTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    public void updateCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new IllegalArgumentException("cron expression must not be blank");
        }
        this.cronExpression = cronExpression;
    }

    public void recordExecution(Instant runTime) {
        this.lastRunTime = runTime;
    }

    public void recordNextRunTime(Instant nextRunTime) {
        this.nextRunTime = nextRunTime;
    }

    public Long getId() { return id; }
    public String getSupplierName() { return supplierName; }
    public String getCronExpression() { return cronExpression; }
    public boolean isEnabled() { return enabled; }
    public Instant getLastRunTime() { return lastRunTime; }
    public Instant getNextRunTime() { return nextRunTime; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
