package com.takarub.esim.supplier.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One package-level delta produced by a supplier sync run.
 * Linked to {@link SupplierSyncAuditLog} (1 audit → many changes).
 */
public class SyncChangeLog {

    private Long id;
    private final Long syncAuditLogId;
    private final String supplier;
    private final String remoteProductId;
    private final String catalogPackageId;
    private final SyncChangeType changeType;
    private final BigDecimal oldCostPrice;
    private final BigDecimal newCostPrice;
    private final String oldCostCurrency;
    private final String newCostCurrency;
    private final BigDecimal oldNormalizedCost;
    private final BigDecimal newNormalizedCost;
    private final String oldNormalizedCurrency;
    private final String newNormalizedCurrency;
    private final Boolean oldInStock;
    private final Boolean newInStock;
    private final Instant changedAt;

    public SyncChangeLog(Long syncAuditLogId,
                         String supplier,
                         String remoteProductId,
                         String catalogPackageId,
                         SyncChangeType changeType,
                         BigDecimal oldCostPrice,
                         BigDecimal newCostPrice,
                         String oldCostCurrency,
                         String newCostCurrency,
                         BigDecimal oldNormalizedCost,
                         BigDecimal newNormalizedCost,
                         String oldNormalizedCurrency,
                         String newNormalizedCurrency,
                         Boolean oldInStock,
                         Boolean newInStock,
                         Instant changedAt) {
        this.syncAuditLogId = syncAuditLogId;
        this.supplier = supplier;
        this.remoteProductId = remoteProductId;
        this.catalogPackageId = catalogPackageId;
        this.changeType = changeType;
        this.oldCostPrice = oldCostPrice;
        this.newCostPrice = newCostPrice;
        this.oldCostCurrency = oldCostCurrency;
        this.newCostCurrency = newCostCurrency;
        this.oldNormalizedCost = oldNormalizedCost;
        this.newNormalizedCost = newNormalizedCost;
        this.oldNormalizedCurrency = oldNormalizedCurrency;
        this.newNormalizedCurrency = newNormalizedCurrency;
        this.oldInStock = oldInStock;
        this.newInStock = newInStock;
        this.changedAt = changedAt;
    }

    public SyncChangeLog(Long id,
                         Long syncAuditLogId,
                         String supplier,
                         String remoteProductId,
                         String catalogPackageId,
                         SyncChangeType changeType,
                         BigDecimal oldCostPrice,
                         BigDecimal newCostPrice,
                         String oldCostCurrency,
                         String newCostCurrency,
                         BigDecimal oldNormalizedCost,
                         BigDecimal newNormalizedCost,
                         String oldNormalizedCurrency,
                         String newNormalizedCurrency,
                         Boolean oldInStock,
                         Boolean newInStock,
                         Instant changedAt) {
        this(syncAuditLogId, supplier, remoteProductId, catalogPackageId, changeType,
                oldCostPrice, newCostPrice, oldCostCurrency, newCostCurrency,
                oldNormalizedCost, newNormalizedCost, oldNormalizedCurrency, newNormalizedCurrency,
                oldInStock, newInStock, changedAt);
        this.id = id;
    }

    public static SyncChangeLog created(Long syncAuditLogId, String supplier, String remoteProductId,
                                        String catalogPackageId,
                                        BigDecimal newCostPrice, String newCostCurrency,
                                        BigDecimal newNormalizedCost, String newNormalizedCurrency,
                                        Instant changedAt) {
        return new SyncChangeLog(
                syncAuditLogId, supplier, remoteProductId, catalogPackageId, SyncChangeType.CREATED,
                null, newCostPrice, null, newCostCurrency,
                null, newNormalizedCost, null, newNormalizedCurrency,
                null, true, changedAt);
    }

    public static SyncChangeLog costUpdated(Long syncAuditLogId, String supplier, String remoteProductId,
                                            String catalogPackageId,
                                            BigDecimal oldCostPrice, BigDecimal newCostPrice,
                                            String oldCostCurrency, String newCostCurrency,
                                            BigDecimal oldNormalizedCost, BigDecimal newNormalizedCost,
                                            String oldNormalizedCurrency, String newNormalizedCurrency,
                                            Instant changedAt) {
        return new SyncChangeLog(
                syncAuditLogId, supplier, remoteProductId, catalogPackageId, SyncChangeType.COST_UPDATED,
                oldCostPrice, newCostPrice, oldCostCurrency, newCostCurrency,
                oldNormalizedCost, newNormalizedCost, oldNormalizedCurrency, newNormalizedCurrency,
                null, null, changedAt);
    }

    public static SyncChangeLog stockOut(Long syncAuditLogId, String supplier, String remoteProductId,
                                         String catalogPackageId,
                                         BigDecimal costPrice, String costCurrency,
                                         BigDecimal normalizedCost, String normalizedCurrency,
                                         Instant changedAt) {
        return new SyncChangeLog(
                syncAuditLogId, supplier, remoteProductId, catalogPackageId, SyncChangeType.STOCK_OUT,
                costPrice, costPrice, costCurrency, costCurrency,
                normalizedCost, normalizedCost, normalizedCurrency, normalizedCurrency,
                true, false, changedAt);
    }

    public Long getId() { return id; }
    public Long getSyncAuditLogId() { return syncAuditLogId; }
    public String getSupplier() { return supplier; }
    public String getRemoteProductId() { return remoteProductId; }
    public String getCatalogPackageId() { return catalogPackageId; }
    public SyncChangeType getChangeType() { return changeType; }
    public BigDecimal getOldCostPrice() { return oldCostPrice; }
    public BigDecimal getNewCostPrice() { return newCostPrice; }
    public String getOldCostCurrency() { return oldCostCurrency; }
    public String getNewCostCurrency() { return newCostCurrency; }
    public BigDecimal getOldNormalizedCost() { return oldNormalizedCost; }
    public BigDecimal getNewNormalizedCost() { return newNormalizedCost; }
    public String getOldNormalizedCurrency() { return oldNormalizedCurrency; }
    public String getNewNormalizedCurrency() { return newNormalizedCurrency; }
    public Boolean getOldInStock() { return oldInStock; }
    public Boolean getNewInStock() { return newInStock; }
    public Instant getChangedAt() { return changedAt; }
}
