package com.takarub.esim.commerce.application.port;

import java.math.BigDecimal;

/**
 * Trusted normalized verification facts from {@link PaymentVerifier}. Not accepted directly from
 * HTTP.
 */
public record VerifiedPaymentResult(
        VerifiedPaymentOutcome outcome,
        BigDecimal verifiedAmount,
        String verifiedCurrency,
        String externalOrderId,
        String externalTransactionId) {
}
