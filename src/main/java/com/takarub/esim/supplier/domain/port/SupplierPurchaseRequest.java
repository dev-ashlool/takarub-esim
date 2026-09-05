package com.takarub.esim.supplier.domain.port;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Purchase request using frozen fulfillment sourcing. {@code fulfillmentWorkId} is correlation only
 * and is not a supplier idempotency key.
 */
public record SupplierPurchaseRequest(
        String supplierKey,
        String remoteProductId,
        String fulfillmentWorkId) {

    public SupplierPurchaseRequest {
        requireText(supplierKey, "supplierKey");
        requireText(remoteProductId, "remoteProductId");
        requireText(fulfillmentWorkId, "fulfillmentWorkId");
        supplierKey = supplierKey.trim();
        remoteProductId = remoteProductId.trim();
        fulfillmentWorkId = fulfillmentWorkId.trim();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
    }
}
