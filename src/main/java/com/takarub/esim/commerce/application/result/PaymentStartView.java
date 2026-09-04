package com.takarub.esim.commerce.application.result;

import java.math.BigDecimal;
import java.time.Instant;

import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;

/**
 * Application result of starting or reusing a payment attempt for an Order.
 *
 * <p>{@code created} is {@code true} when this invocation created a new attempt;
 * {@code false} when an existing active {@code INITIATED} attempt was reused.
 */
public record PaymentStartView(
        PaymentAttemptId paymentAttemptId,
        OrderId orderId,
        PaymentAttemptStatus paymentAttemptStatus,
        OrderStatus orderStatus,
        BigDecimal amount,
        String currency,
        String externalOrderId,
        String externalTransactionId,
        Instant createdAt,
        Instant updatedAt,
        boolean created) {

    public static PaymentStartView from(Order order, PaymentAttempt attempt, boolean created) {
        return new PaymentStartView(
                attempt.id(),
                order.id(),
                attempt.status(),
                order.status(),
                attempt.amount(),
                attempt.currency(),
                attempt.externalOrderId(),
                attempt.externalTransactionId(),
                attempt.createdAt(),
                attempt.updatedAt(),
                created);
    }
}
