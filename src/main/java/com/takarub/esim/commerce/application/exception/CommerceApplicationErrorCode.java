package com.takarub.esim.commerce.application.exception;

import com.takarub.esim.identity.shared.exception.ErrorCode;

/**
 * Application-layer error codes for the Commerce module. Orchestration-level codes (lookup
 * failures, unsellable packages) kept separate from domain validation exceptions.
 */
public enum CommerceApplicationErrorCode implements ErrorCode {

    CART_NOT_FOUND("COMMERCE_CART_NOT_FOUND", "The requested cart was not found."),
    ORDER_NOT_FOUND("COMMERCE_ORDER_NOT_FOUND", "The requested order was not found."),
    PAYMENT_ATTEMPT_NOT_FOUND("COMMERCE_PAYMENT_ATTEMPT_NOT_FOUND",
            "The requested payment attempt was not found."),
    PACKAGE_NOT_SELLABLE("COMMERCE_PACKAGE_NOT_SELLABLE",
            "The catalog package is not available for purchase."),
    NO_SUPPLIER_PRODUCT_AVAILABLE("COMMERCE_NO_SUPPLIER_PRODUCT_AVAILABLE",
            "No in-stock supplier product is available for this package."),
    ESIM_NOT_READY("COMMERCE_ESIM_NOT_READY",
            "eSIM activation data is not ready for this order.");

    private final String code;
    private final String defaultMessage;

    CommerceApplicationErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
