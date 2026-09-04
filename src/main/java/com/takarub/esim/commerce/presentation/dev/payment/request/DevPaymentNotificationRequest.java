package com.takarub.esim.commerce.presentation.dev.payment.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Untrusted dev notification trigger. Amount, currency, and payment/order status are never accepted.
 */
public record DevPaymentNotificationRequest(
        @NotBlank(message = "paymentAttemptId is required") String paymentAttemptId,
        @NotBlank(message = "verificationReference is required") String verificationReference) {
}
