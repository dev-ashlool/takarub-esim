package com.takarub.esim.commerce.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Immutable read model projecting an {@link Order} for application responses.
 */
public record OrderView(
        OrderId id,
        CartId cartId,
        UserId userId,
        OrderStatus status,
        List<OrderItemView> items,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderView from(Order order) {
        List<OrderItemView> items = order.itemsView().stream()
                .map(OrderItemView::from)
                .toList();

        return new OrderView(
                order.id(),
                order.cartId(),
                order.userId(),
                order.status(),
                items,
                order.totalAmount(),
                order.currency(),
                order.createdAt(),
                order.updatedAt());
    }
}
