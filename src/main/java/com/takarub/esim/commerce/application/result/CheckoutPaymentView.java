package com.takarub.esim.commerce.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;

/**
 * Provider-neutral combined result of initial checkout + payment start (or idempotent reuse).
 */
public record CheckoutPaymentView(
        OrderId orderId,
        OrderStatus orderStatus,
        List<OrderItemView> items,
        BigDecimal totalAmount,
        String currency,
        Instant orderCreatedAt,
        Instant orderUpdatedAt,
        PaymentAttemptId paymentAttemptId,
        PaymentAttemptStatus paymentAttemptStatus,
        String externalOrderId,
        String externalTransactionId,
        Instant paymentCreatedAt,
        Instant paymentUpdatedAt) {

    public static CheckoutPaymentView from(Order order, PaymentAttempt attempt) {
        List<OrderItemView> items = order.itemsView().stream()
                .map(OrderItemView::from)
                .toList();
        return new CheckoutPaymentView(
                order.id(),
                order.status(),
                items,
                order.totalAmount(),
                order.currency(),
                order.createdAt(),
                order.updatedAt(),
                attempt.id(),
                attempt.status(),
                attempt.externalOrderId(),
                attempt.externalTransactionId(),
                attempt.createdAt(),
                attempt.updatedAt());
    }
}
