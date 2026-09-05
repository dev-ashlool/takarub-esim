package com.takarub.esim.commerce.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.commerce.application.port.PaymentVerifier;
import com.takarub.esim.commerce.application.usecase.HandlePaymentNotificationUseCase;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptRepository;
import com.takarub.esim.commerce.infrastructure.verification.FakePaymentVerifier;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Conditional wiring for local/dev payment verification. Absent or false property means no fake
 * verifier and no notification use-case bean — production starts without {@link PaymentVerifier}.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "takarub.commerce.dev-payment-verification",
        name = "enabled",
        havingValue = "true")
public class CommerceDevPaymentVerificationConfig {

    @Bean
    public FakePaymentVerifier fakePaymentVerifier() {
        return new FakePaymentVerifier();
    }

    @Bean
    public HandlePaymentNotificationUseCase handlePaymentNotificationUseCase(
            TransactionRunner transactionRunner,
            PaymentAttemptRepository paymentAttemptRepository,
            OrderRepository orderRepository,
            FulfillmentWorkRepository fulfillmentWorkRepository,
            PaymentVerifier paymentVerifier,
            IdGenerator idGenerator,
            ClockProvider clockProvider) {
        return new HandlePaymentNotificationUseCase(
                transactionRunner,
                paymentAttemptRepository,
                orderRepository,
                fulfillmentWorkRepository,
                paymentVerifier,
                idGenerator,
                clockProvider);
    }
}
