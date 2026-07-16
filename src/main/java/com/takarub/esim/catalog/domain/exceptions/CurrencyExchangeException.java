package com.takarub.esim.catalog.domain.exceptions;

/**
 * Thrown when currency normalization cannot convert an amount (e.g. missing FX rate in DB).
 */
public final class CurrencyExchangeException extends RuntimeException {

    public CurrencyExchangeException(String message) {
        super(message);
    }

    public CurrencyExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
