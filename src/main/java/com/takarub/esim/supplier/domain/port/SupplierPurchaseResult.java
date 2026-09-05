package com.takarub.esim.supplier.domain.port;

import java.util.Objects;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Normalized supplier purchase result. Validates outcome-specific invariants at construction.
 */
public record SupplierPurchaseResult(
        SupplierPurchaseOutcome outcome,
        SupplierPurchaseSuccessPayload successPayload,
        String errorCode,
        String errorMessage) {

    public SupplierPurchaseResult {
        Objects.requireNonNull(outcome, "outcome must not be null");
        switch (outcome) {
            case SUCCEEDED -> {
                if (successPayload == null) {
                    throw new ValidationException("successPayload is required for SUCCEEDED");
                }
                requireText(successPayload.supplierOrderId(), "supplierOrderId");
                requireUsableActivationProof(successPayload);
                requireAbsent(errorCode, "errorCode");
                requireAbsent(errorMessage, "errorMessage");
            }
            case FAILED, UNKNOWN -> {
                if (successPayload != null) {
                    throw new ValidationException(
                            "successPayload must be absent for " + outcome);
                }
                requireText(errorCode, "errorCode");
                requireText(errorMessage, "errorMessage");
                errorCode = errorCode.trim();
                errorMessage = errorMessage.trim();
            }
            default -> throw new ValidationException("Unsupported purchase outcome: " + outcome);
        }
    }

    public static SupplierPurchaseResult succeeded(SupplierPurchaseSuccessPayload payload) {
        return new SupplierPurchaseResult(SupplierPurchaseOutcome.SUCCEEDED, payload, null, null);
    }

    public static SupplierPurchaseResult failed(String errorCode, String errorMessage) {
        return new SupplierPurchaseResult(
                SupplierPurchaseOutcome.FAILED, null, errorCode, errorMessage);
    }

    public static SupplierPurchaseResult unknown(String errorCode, String errorMessage) {
        return new SupplierPurchaseResult(
                SupplierPurchaseOutcome.UNKNOWN, null, errorCode, errorMessage);
    }

    private static void requireUsableActivationProof(SupplierPurchaseSuccessPayload payload) {
        String qr = blankToNull(payload.qrString());
        String smdp = blankToNull(payload.smdpAddress());
        String activation = blankToNull(payload.activationCode());
        boolean hasQr = qr != null;
        boolean hasSmdpPair = smdp != null && activation != null;
        if (!hasQr && !hasSmdpPair) {
            throw new ValidationException(
                    "Usable activation proof requires qrString or smdpAddress+activationCode");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
    }

    private static void requireAbsent(String value, String field) {
        if (value != null && !value.isBlank()) {
            throw new ValidationException(field + " must be null");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
