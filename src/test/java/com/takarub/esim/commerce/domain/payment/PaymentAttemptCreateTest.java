package com.takarub.esim.commerce.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

class PaymentAttemptCreateTest {

    private static final Instant T0 = Instant.parse("2026-03-01T10:00:00Z");

    private final IdGenerator idGenerator = new UuidIdGenerator();
    private final AtomicReference<Instant> now = new AtomicReference<>(T0);
    private final ClockProvider clock = new MutableClockProvider(now);

    private OrderId orderId;

    @BeforeEach
    void setUp() {
        now.set(T0);
        orderId = OrderId.of(UUID.randomUUID());
    }

    @Test
    void createGeneratesIdAndInitiatedStatus() {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("10.00"), "USD");

        assertThat(attempt.id()).isNotNull();
        assertThat(attempt.id().value()).isNotNull();
        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.INITIATED);
    }

    @Test
    void createStoresOrderIdAmountAndCurrency() {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("19.99"), "USD");

        assertThat(attempt.orderId()).isEqualTo(orderId);
        assertThat(attempt.amount()).isEqualByComparingTo("19.99");
        assertThat(attempt.currency()).isEqualTo("USD");
    }

    @Test
    void createNormalizesAmountToScaleTwoHalfUp() {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("10.005"), "USD");

        assertThat(attempt.amount()).isEqualByComparingTo("10.01");
        assertThat(attempt.amount().scale()).isEqualTo(2);
    }

    @Test
    void createTrimsCurrency() {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("10.00"), "  USD  ");

        assertThat(attempt.currency()).isEqualTo("USD");
    }

    @Test
    void createLeavesExternalIdsNull() {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("10.00"), "USD");

        assertThat(attempt.externalOrderId()).isNull();
        assertThat(attempt.externalTransactionId()).isNull();
    }

    @Test
    void createSetsTimestampsFromClock() {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("10.00"), "USD");

        assertThat(attempt.createdAt()).isEqualTo(T0);
        assertThat(attempt.updatedAt()).isEqualTo(T0);
    }

    @Test
    void nullOrderIdRejected() {
        assertThatThrownBy(() -> PaymentAttempt.create(
                idGenerator, clock, null, new BigDecimal("10.00"), "USD"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nullAmountRejected() {
        assertThatThrownBy(() -> PaymentAttempt.create(
                idGenerator, clock, orderId, null, "USD"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void zeroAmountRejected() {
        assertThatThrownBy(() -> PaymentAttempt.create(
                idGenerator, clock, orderId, BigDecimal.ZERO, "USD"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void negativeAmountRejected() {
        assertThatThrownBy(() -> PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("-1.00"), "USD"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nullCurrencyRejected() {
        assertThatThrownBy(() -> PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("10.00"), null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void blankCurrencyRejected() {
        assertThatThrownBy(() -> PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("10.00"), "   "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void equalityAndHashCodeAreIdBased() {
        PaymentAttempt first = PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("10.00"), "USD");
        PaymentAttempt sameId = PaymentAttempt.reconstitute(
                first.id(),
                first.createdAt(),
                first.updatedAt(),
                OrderId.of(UUID.randomUUID()),
                PaymentAttemptStatus.FAILED,
                new BigDecimal("99.00"),
                "EUR",
                "ext-o",
                "ext-t");

        assertThat(first).isEqualTo(sameId);
        assertThat(first.hashCode()).isEqualTo(sameId.hashCode());
        assertThat(first).isNotEqualTo(PaymentAttempt.create(
                idGenerator, clock, orderId, new BigDecimal("10.00"), "USD"));
    }

    private static final class MutableClockProvider implements ClockProvider {

        private final AtomicReference<Instant> instant;

        private MutableClockProvider(AtomicReference<Instant> instant) {
            this.instant = instant;
        }

        @Override
        public Clock getClock() {
            return Clock.fixed(instant.get(), ZoneOffset.UTC);
        }

        @Override
        public Instant now() {
            return instant.get();
        }

        @Override
        public LocalDateTime localDateTimeNow() {
            return LocalDateTime.ofInstant(instant.get(), ZoneOffset.UTC);
        }

        @Override
        public LocalDate today() {
            return localDateTimeNow().toLocalDate();
        }

        @Override
        public ZoneId zone() {
            return ZoneOffset.UTC;
        }
    }
}
