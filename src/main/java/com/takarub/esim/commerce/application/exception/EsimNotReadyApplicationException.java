package com.takarub.esim.commerce.application.exception;

import com.takarub.esim.identity.shared.exception.ConflictException;

/**
 * Thrown when an owned order exists but eSIM activation data is not yet available to the customer.
 * Maps to HTTP 409 via {@code GlobalExceptionHandler}. Public message must not expose supplier or
 * activation internals.
 */
public class EsimNotReadyApplicationException extends ConflictException {

    public EsimNotReadyApplicationException() {
        super(CommerceApplicationErrorCode.ESIM_NOT_READY,
                CommerceApplicationErrorCode.ESIM_NOT_READY.defaultMessage());
    }
}
