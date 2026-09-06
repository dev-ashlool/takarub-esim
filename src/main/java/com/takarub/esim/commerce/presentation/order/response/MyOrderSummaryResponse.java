package com.takarub.esim.commerce.presentation.order.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Customer My Orders list row.
 */
public record MyOrderSummaryResponse(
        String orderId,
        String orderStatus,
        String fulfillmentStatus,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        List<CustomerOrderItemResponse> items) {
}
