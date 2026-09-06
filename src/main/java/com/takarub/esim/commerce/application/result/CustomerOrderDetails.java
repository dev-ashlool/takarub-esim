package com.takarub.esim.commerce.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;

/**
 * Customer-facing Order Details read model. Omits supplier and provisioning secrets.
 */
public record CustomerOrderDetails(
        OrderId orderId,
        OrderStatus orderStatus,
        FulfillmentStatus fulfillmentStatus,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemView> items) {
}
