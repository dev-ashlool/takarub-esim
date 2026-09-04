package com.takarub.esim.commerce.application.result;

import java.math.BigDecimal;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;

/**
 * Provider-neutral result of handling a payment verification notification.
 */
public record PaymentVerificationView(
        PaymentAttemptId paymentAttemptId,
        OrderId orderId,
        PaymentAttemptStatus paymentAttemptStatus,
        OrderStatus orderStatus,
        BigDecimal amount,
        String currency,
        PaymentVerificationDisposition disposition) {
}
