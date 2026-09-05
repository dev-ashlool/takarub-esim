package com.takarub.esim.supplier.domain.port;

/**
 * Success payload for a supplier purchase. Usable activation proof is validated by
 * {@link SupplierPurchaseResult}.
 */
public record SupplierPurchaseSuccessPayload(
        String supplierOrderId,
        String iccid,
        String smdpAddress,
        String activationCode,
        String pin,
        String puk,
        String qrString) {
}
