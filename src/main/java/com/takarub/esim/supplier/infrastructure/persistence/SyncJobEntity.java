package com.takarub.esim.supplier.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sync_jobs")
public class SyncJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_name", length = 50, nullable = false, unique = true)
    private String supplierName;

    @Column(name = "cron_expression", length = 50, nullable = false)
    private String cronExpression;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "last_run_time")
    private Instant lastRunTime;

    @Column(name = "next_run_time")
    private Instant nextRunTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SyncJobEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getLastRunTime() { return lastRunTime; }
    public void setLastRunTime(Instant lastRunTime) { this.lastRunTime = lastRunTime; }
    public Instant getNextRunTime() { return nextRunTime; }
    public void setNextRunTime(Instant nextRunTime) { this.nextRunTime = nextRunTime; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
