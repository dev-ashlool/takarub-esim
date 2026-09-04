package com.takarub.esim.commerce.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.order.OrderId;

class PaymentAttemptReconstituteTest {

    @Test
    void reconstitutesInitiatedWithNullExternalIds() {
        PaymentAttemptId id = PaymentAttemptId.of(UUID.randomUUID());
        OrderId orderId = OrderId.of(UUID.randomUUID());
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-01T00:00:00Z");
        BigDecimal amount = new BigDecimal("12.50");

        PaymentAttempt attempt = PaymentAttempt.reconstitute(
                id,
                createdAt,
                updatedAt,
                orderId,
                PaymentAttemptStatus.INITIATED,
                amount,
                "USD",
                null,
                null);

        assertThat(attempt.id()).isEqualTo(id);
        assertThat(attempt.orderId()).isEqualTo(orderId);
        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(attempt.amount()).isEqualByComparingTo(amount);
        assertThat(attempt.currency()).isEqualTo("USD");
        assertThat(attempt.externalOrderId()).isNull();
        assertThat(attempt.externalTransactionId()).isNull();
        assertThat(attempt.createdAt()).isEqualTo(createdAt);
        assertThat(attempt.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void reconstitutesInitiatedWithExternalIds() {
        PaymentAttempt attempt = PaymentAttempt.reconstitute(
                PaymentAttemptId.of(UUID.randomUUID()),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                OrderId.of(UUID.randomUUID()),
                PaymentAttemptStatus.INITIATED,
                new BigDecimal("12.50"),
                "USD",
                "ext-order",
                "ext-tx");

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(attempt.externalOrderId()).isEqualTo("ext-order");
        assertThat(attempt.externalTransactionId()).isEqualTo("ext-tx");
        assertThat(attempt.updatedAt()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void reconstitutesConfirmedPreservesExactState() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-03T08:00:00Z");

        PaymentAttempt attempt = PaymentAttempt.reconstitute(
                PaymentAttemptId.of(UUID.randomUUID()),
                createdAt,
                updatedAt,
                OrderId.of(UUID.randomUUID()),
                PaymentAttemptStatus.CONFIRMED,
                new BigDecimal("29.99"),
                "EUR",
                "ext-order",
                "ext-tx");

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
        assertThat(attempt.amount()).isEqualByComparingTo("29.99");
        assertThat(attempt.currency()).isEqualTo("EUR");
        assertThat(attempt.externalOrderId()).isEqualTo("ext-order");
        assertThat(attempt.externalTransactionId()).isEqualTo("ext-tx");
        assertThat(attempt.createdAt()).isEqualTo(createdAt);
        assertThat(attempt.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void reconstitutesFailedPreservesExactState() {
        PaymentAttempt attempt = PaymentAttempt.reconstitute(
                PaymentAttemptId.of(UUID.randomUUID()),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-04T12:00:00Z"),
                OrderId.of(UUID.randomUUID()),
                PaymentAttemptStatus.FAILED,
                new BigDecimal("5.00"),
                "USD",
                null,
                "ext-tx-only");

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(attempt.externalOrderId()).isNull();
        assertThat(attempt.externalTransactionId()).isEqualTo("ext-tx-only");
    }
}
