package com.takarub.esim.commerce.presentation.dev.payment.response;

import java.math.BigDecimal;

/**
 * Provider-neutral HTTP response for a handled payment verification notification.
 */
public record DevPaymentNotificationResponse(
        String paymentAttemptId,
        String orderId,
        String paymentAttemptStatus,
        String orderStatus,
        BigDecimal amount,
        String currency,
        String disposition) {
}
