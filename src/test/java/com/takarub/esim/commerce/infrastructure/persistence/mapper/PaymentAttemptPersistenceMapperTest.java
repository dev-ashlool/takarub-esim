package com.takarub.esim.commerce.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.commerce.infrastructure.persistence.entity.PaymentAttemptJpaEntity;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

/**
 * Pure mapper round-trip (no Spring / DB). Complements adapter DataJpaTest when Flyway+H2 cannot
 * boot the full persistence context.
 */
class PaymentAttemptPersistenceMapperTest {

    private static final Instant FIXED = Instant.parse("2026-09-04T12:00:00Z");

    private final PaymentAttemptPersistenceMapper mapper = new PaymentAttemptPersistenceMapper();
    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final ClockProvider clock = new SystemClockProvider(Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void roundTripsInitiatedWithNullExternalIds() {
        OrderId orderId = OrderId.of(UUID.randomUUID());
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("19.99"), "USD");

        PaymentAttemptJpaEntity entity = mapper.toEntity(attempt);
        PaymentAttempt reconstituted = mapper.toDomain(entity);

        assertThat(entity.getId()).isEqualTo(attempt.id().value().toString());
        assertThat(entity.getOrderId()).isEqualTo(orderId.value().toString());
        assertThat(entity.getStatus()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(entity.getExternalOrderId()).isNull();
        assertThat(entity.getExternalTransactionId()).isNull();

        assertThat(reconstituted.id()).isEqualTo(attempt.id());
        assertThat(reconstituted.orderId()).isEqualTo(orderId);
        assertThat(reconstituted.status()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(reconstituted.amount()).isEqualByComparingTo("19.99");
        assertThat(reconstituted.currency()).isEqualTo("USD");
        assertThat(reconstituted.externalOrderId()).isNull();
        assertThat(reconstituted.externalTransactionId()).isNull();
        assertThat(reconstituted.createdAt()).isEqualTo(FIXED);
        assertThat(reconstituted.updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void roundTripsInitiatedWithExternalIds() {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, OrderId.of(UUID.randomUUID()), new BigDecimal("10.00"), "EUR");
        attempt.assignExternalOrderId(clock, "ext-order-1");
        attempt.assignExternalTransactionId(clock, "ext-tx-1");

        PaymentAttempt reconstituted = mapper.toDomain(mapper.toEntity(attempt));

        assertThat(reconstituted.status()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(reconstituted.externalOrderId()).isEqualTo("ext-order-1");
        assertThat(reconstituted.externalTransactionId()).isEqualTo("ext-tx-1");
        assertThat(reconstituted.amount()).isEqualByComparingTo("10.00");
        assertThat(reconstituted.currency()).isEqualTo("EUR");
    }

    @Test
    void roundTripsConfirmed() {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, OrderId.of(UUID.randomUUID()), new BigDecimal("5.50"), "USD");
        attempt.assignExternalOrderId(clock, "ext-order");
        attempt.confirm(clock);

        PaymentAttempt reconstituted = mapper.toDomain(mapper.toEntity(attempt));

        assertThat(reconstituted.status()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
        assertThat(reconstituted.externalOrderId()).isEqualTo("ext-order");
        assertThat(reconstituted.externalTransactionId()).isNull();
        assertThat(reconstituted.createdAt()).isEqualTo(FIXED);
        assertThat(reconstituted.updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void roundTripsFailed() {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, OrderId.of(UUID.randomUUID()), new BigDecimal("7.25"), "USD");
        attempt.fail(clock);

        PaymentAttempt reconstituted = mapper.toDomain(mapper.toEntity(attempt));

        assertThat(reconstituted.status()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(reconstituted.amount()).isEqualByComparingTo("7.25");
        assertThat(reconstituted.currency()).isEqualTo("USD");
        assertThat(reconstituted.externalOrderId()).isNull();
        assertThat(reconstituted.externalTransactionId()).isNull();
    }
}
