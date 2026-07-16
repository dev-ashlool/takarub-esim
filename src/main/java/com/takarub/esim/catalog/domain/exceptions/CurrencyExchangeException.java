package com.takarub.esim.catalog.domain.exceptions;

/**
 * Thrown when currency normalization encounters an unmapped currency token or missing cache mapping.
 */
public final class CurrencyExchangeException extends RuntimeException {

    public CurrencyExchangeException(String message) {
        super(message);
    }

    public CurrencyExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
