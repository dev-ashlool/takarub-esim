package com.takarub.esim.commerce.presentation.order.payment.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * REST representation of an explicit start/retry payment result. Provider-neutral; omits PSP
 * session fields.
 */
public record PaymentStartResponse(
        String paymentAttemptId,
        String orderId,
        String paymentAttemptStatus,
        String orderStatus,
        BigDecimal amount,
        String currency,
        String externalOrderId,
        String externalTransactionId,
        Instant createdAt,
        Instant updatedAt,
        boolean created) {
}
