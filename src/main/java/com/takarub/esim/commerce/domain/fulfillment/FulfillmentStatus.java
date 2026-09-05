package com.takarub.esim.commerce.domain.fulfillment;

/**
 * Lifecycle of durable fulfillment work after payment. B1 uses {@link #PENDING} and
 * {@link #BLOCKED}; remaining values are reserved for the execution engine (B2).
 */
public enum FulfillmentStatus {
    /** Ready for a first supplier purchase attempt. */
    PENDING,
    /** Exclusively claimed; supplier call may be in progress. */
    PROCESSING,
    /** Proven successful provisioning. */
    FULFILLED,
    /** Supplier call may have succeeded; outcome cannot be proven. */
    UNKNOWN,
    /** Local/data/operational blocker prevents a safe supplier attempt. */
    BLOCKED
}
