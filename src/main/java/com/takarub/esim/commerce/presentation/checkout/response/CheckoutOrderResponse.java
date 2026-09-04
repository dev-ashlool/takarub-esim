package com.takarub.esim.commerce.presentation.checkout.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * REST representation of a checkout Order result. Omits cartId, userId, and checkoutRequestId.
 */
public record CheckoutOrderResponse(
        String orderId,
        String status,
        List<CheckoutOrderItemResponse> items,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt) {
}
