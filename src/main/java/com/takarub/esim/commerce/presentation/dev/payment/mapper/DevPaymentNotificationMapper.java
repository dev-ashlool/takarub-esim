package com.takarub.esim.commerce.presentation.dev.payment.mapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.application.command.HandlePaymentNotificationCommand;
import com.takarub.esim.commerce.application.result.PaymentVerificationView;
import com.takarub.esim.commerce.presentation.dev.payment.request.DevPaymentNotificationRequest;
import com.takarub.esim.commerce.presentation.dev.payment.response.DevPaymentNotificationResponse;

@Component
@ConditionalOnProperty(
        prefix = "takarub.commerce.dev-payment-verification",
        name = "enabled",
        havingValue = "true")
public class DevPaymentNotificationMapper {

    public HandlePaymentNotificationCommand toCommand(DevPaymentNotificationRequest request) {
        return new HandlePaymentNotificationCommand(
                request.paymentAttemptId(),
                request.verificationReference());
    }

    public DevPaymentNotificationResponse toResponse(PaymentVerificationView view) {
        return new DevPaymentNotificationResponse(
                view.paymentAttemptId().value().toString(),
                view.orderId().value().toString(),
                view.paymentAttemptStatus().name(),
                view.orderStatus().name(),
                view.amount(),
                view.currency(),
                view.disposition().name());
    }
}
