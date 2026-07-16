package com.takarub.esim.supplier.application.port;

import java.time.Instant;

import com.takarub.esim.supplier.domain.model.RawSupplierProduct;

/**
 * Persists raw LikeCard catalog payloads for audit and downstream reconciliation.
 */
public interface SupplierLikeCardProductLogPort {

    void saveOrUpdate(RawSupplierProduct parsed, Instant syncedAt);
}
