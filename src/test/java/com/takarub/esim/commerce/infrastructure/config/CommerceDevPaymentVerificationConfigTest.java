package com.takarub.esim.commerce.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.takarub.esim.commerce.application.port.PaymentVerifier;
import com.takarub.esim.commerce.application.usecase.HandlePaymentNotificationUseCase;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptRepository;
import com.takarub.esim.commerce.infrastructure.verification.FakePaymentVerifier;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.time.ClockProvider;

class CommerceDevPaymentVerificationConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CommerceDevPaymentVerificationConfig.class)
            .withBean(TransactionRunner.class, () -> mock(TransactionRunner.class))
            .withBean(PaymentAttemptRepository.class, () -> mock(PaymentAttemptRepository.class))
            .withBean(OrderRepository.class, () -> mock(OrderRepository.class))
            .withBean(ClockProvider.class, () -> mock(ClockProvider.class));

    @Test
    void propertyAbsentDoesNotActivateDevVerificationBeans() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(FakePaymentVerifier.class);
            assertThat(context).doesNotHaveBean(PaymentVerifier.class);
            assertThat(context).doesNotHaveBean(HandlePaymentNotificationUseCase.class);
            assertThat(context).doesNotHaveBean(CommerceDevPaymentVerificationConfig.class);
        });
    }

    @Test
    void propertyFalseDoesNotActivateDevVerificationBeans() {
        runner.withPropertyValues("takarub.commerce.dev-payment-verification.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FakePaymentVerifier.class);
                    assertThat(context).doesNotHaveBean(HandlePaymentNotificationUseCase.class);
                });
    }

    @Test
    void propertyTrueActivatesFakeVerifierAndUseCase() {
        runner.withPropertyValues("takarub.commerce.dev-payment-verification.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(FakePaymentVerifier.class);
                    assertThat(context).hasSingleBean(PaymentVerifier.class);
                    assertThat(context).hasSingleBean(HandlePaymentNotificationUseCase.class);
                    assertThat(context.getBean(PaymentVerifier.class))
                            .isInstanceOf(FakePaymentVerifier.class);
                });
    }
}
