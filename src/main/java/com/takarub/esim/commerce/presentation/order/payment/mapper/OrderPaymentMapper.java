package com.takarub.esim.commerce.presentation.order.payment.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.application.result.PaymentStartView;
import com.takarub.esim.commerce.presentation.order.payment.response.PaymentStartResponse;

/**
 * Maps {@link PaymentStartView} to the HTTP payment-start response.
 */
@Component
public class OrderPaymentMapper {

    public PaymentStartResponse toResponse(PaymentStartView view) {
        return new PaymentStartResponse(
                view.paymentAttemptId().value().toString(),
                view.orderId().value().toString(),
                view.paymentAttemptStatus().name(),
                view.orderStatus().name(),
                view.amount(),
                view.currency(),
                view.externalOrderId(),
                view.externalTransactionId(),
                view.createdAt(),
                view.updatedAt(),
                view.created());
    }
}
