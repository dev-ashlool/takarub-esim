package com.takarub.esim.supplier.domain.exceptions;

/**
 * Raised when a downstream supplier API gateway fails, times out, or returns a logic error flag.
 */
public final class SupplierApiException extends RuntimeException {

    public SupplierApiException(String message) {
        super(message);
    }

    public SupplierApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
