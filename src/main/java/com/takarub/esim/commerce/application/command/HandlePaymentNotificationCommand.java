package com.takarub.esim.commerce.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Untrusted notification input for payment verification. Amount, currency, and payment status are
 * never accepted from the client.
 */
public record HandlePaymentNotificationCommand(String paymentAttemptId, String verificationReference) {

    public HandlePaymentNotificationCommand {
        if (paymentAttemptId == null || paymentAttemptId.isBlank()) {
            throw new ValidationException("Payment attempt id is required");
        }
        if (verificationReference == null || verificationReference.isBlank()) {
            throw new ValidationException("Verification reference is required");
        }
    }
}
