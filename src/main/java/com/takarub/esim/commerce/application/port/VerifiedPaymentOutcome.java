package com.takarub.esim.commerce.application.port;

/**
 * Trusted verification outcome produced only by a {@link PaymentVerifier}.
 */
public enum VerifiedPaymentOutcome {
    SUCCEEDED,
    FAILED
}
