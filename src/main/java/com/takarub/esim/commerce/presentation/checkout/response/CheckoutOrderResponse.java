package com.takarub.esim.commerce.presentation.checkout.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * REST representation of initial checkout + payment-start result. Omits cartId, userId, and
 * checkoutRequestId. Includes provider-neutral payment attempt identifiers only.
 */
public record CheckoutOrderResponse(
        String orderId,
        String status,
        List<CheckoutOrderItemResponse> items,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt,
        String paymentAttemptId,
        String paymentAttemptStatus,
        String externalOrderId,
        String externalTransactionId) {
}
