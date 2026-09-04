package com.takarub.esim.commerce.application.exception;

import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.identity.shared.exception.ResourceNotFoundException;

/**
 * Thrown when a PaymentAttempt cannot be loaded for the requested id.
 */
public class PaymentAttemptNotFoundApplicationException extends ResourceNotFoundException {

    public PaymentAttemptNotFoundApplicationException(PaymentAttemptId paymentAttemptId) {
        super(CommerceApplicationErrorCode.PAYMENT_ATTEMPT_NOT_FOUND,
                "Payment attempt " + paymentAttemptId.value() + " was not found.");
    }
}
