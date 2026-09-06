package com.takarub.esim.commerce.presentation.order.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Customer Order Details response.
 */
public record OrderDetailsResponse(
        String orderId,
        String orderStatus,
        String fulfillmentStatus,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt,
        List<CustomerOrderItemResponse> items) {
}
