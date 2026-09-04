package com.takarub.esim.commerce.application.port;

import java.math.BigDecimal;

import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;

/**
 * Server-built verification request. Expected amount/currency come from the PaymentAttempt, not
 * from the HTTP notification. {@code verificationInput} is an opaque adapter-specific token
 * (used by the current fake verifier; a future PSP adapter may ignore or reinterpret it).
 */
public record PaymentVerificationRequest(
        PaymentAttemptId paymentAttemptId,
        BigDecimal expectedAmount,
        String expectedCurrency,
        String verificationInput) {
}
