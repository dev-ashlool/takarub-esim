package com.takarub.esim.supplier.domain.model;

/**
 * Kind of measurable change recorded for a single package during a supplier sync run.
 */
public enum SyncChangeType {
    CREATED,
    COST_UPDATED,
    STOCK_OUT
}
