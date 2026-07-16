package com.takarub.esim.supplier.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

import com.takarub.esim.supplier.domain.model.SyncChangeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sync_change_logs")
public class SyncChangeLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_audit_log_id", nullable = false)
    private Long syncAuditLogId;

    @Column(name = "supplier", length = 50, nullable = false)
    private String supplier;

    @Column(name = "remote_product_id", length = 50, nullable = false)
    private String remoteProductId;

    @Column(name = "catalog_package_id", length = 36)
    private String catalogPackageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 20, nullable = false)
    private SyncChangeType changeType;

    @Column(name = "old_cost_price", precision = 12, scale = 4)
    private BigDecimal oldCostPrice;

    @Column(name = "new_cost_price", precision = 12, scale = 4)
    private BigDecimal newCostPrice;

    @Column(name = "old_cost_currency", length = 3)
    private String oldCostCurrency;

    @Column(name = "new_cost_currency", length = 3)
    private String newCostCurrency;

    @Column(name = "old_normalized_cost", precision = 12, scale = 4)
    private BigDecimal oldNormalizedCost;

    @Column(name = "new_normalized_cost", precision = 12, scale = 4)
    private BigDecimal newNormalizedCost;

    @Column(name = "old_normalized_currency", length = 3)
    private String oldNormalizedCurrency;

    @Column(name = "new_normalized_currency", length = 3)
    private String newNormalizedCurrency;

    @Column(name = "old_in_stock")
    private Boolean oldInStock;

    @Column(name = "new_in_stock")
    private Boolean newInStock;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected SyncChangeLogEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getSyncAuditLogId() {
        return syncAuditLogId;
    }

    public void setSyncAuditLogId(Long syncAuditLogId) {
        this.syncAuditLogId = syncAuditLogId;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getRemoteProductId() {
        return remoteProductId;
    }

    public void setRemoteProductId(String remoteProductId) {
        this.remoteProductId = remoteProductId;
    }

    public String getCatalogPackageId() {
        return catalogPackageId;
    }

    public void setCatalogPackageId(String catalogPackageId) {
        this.catalogPackageId = catalogPackageId;
    }

    public SyncChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(SyncChangeType changeType) {
        this.changeType = changeType;
    }

    public BigDecimal getOldCostPrice() {
        return oldCostPrice;
    }

    public void setOldCostPrice(BigDecimal oldCostPrice) {
        this.oldCostPrice = oldCostPrice;
    }

    public BigDecimal getNewCostPrice() {
        return newCostPrice;
    }

    public void setNewCostPrice(BigDecimal newCostPrice) {
        this.newCostPrice = newCostPrice;
    }

    public String getOldCostCurrency() {
        return oldCostCurrency;
    }

    public void setOldCostCurrency(String oldCostCurrency) {
        this.oldCostCurrency = oldCostCurrency;
    }

    public String getNewCostCurrency() {
        return newCostCurrency;
    }

    public void setNewCostCurrency(String newCostCurrency) {
        this.newCostCurrency = newCostCurrency;
    }

    public BigDecimal getOldNormalizedCost() {
        return oldNormalizedCost;
    }

    public void setOldNormalizedCost(BigDecimal oldNormalizedCost) {
        this.oldNormalizedCost = oldNormalizedCost;
    }

    public BigDecimal getNewNormalizedCost() {
        return newNormalizedCost;
    }

    public void setNewNormalizedCost(BigDecimal newNormalizedCost) {
        this.newNormalizedCost = newNormalizedCost;
    }

    public String getOldNormalizedCurrency() {
        return oldNormalizedCurrency;
    }

    public void setOldNormalizedCurrency(String oldNormalizedCurrency) {
        this.oldNormalizedCurrency = oldNormalizedCurrency;
    }

    public String getNewNormalizedCurrency() {
        return newNormalizedCurrency;
    }

    public void setNewNormalizedCurrency(String newNormalizedCurrency) {
        this.newNormalizedCurrency = newNormalizedCurrency;
    }

    public Boolean getOldInStock() {
        return oldInStock;
    }

    public void setOldInStock(Boolean oldInStock) {
        this.oldInStock = oldInStock;
    }

    public Boolean getNewInStock() {
        return newInStock;
    }

    public void setNewInStock(Boolean newInStock) {
        this.newInStock = newInStock;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
