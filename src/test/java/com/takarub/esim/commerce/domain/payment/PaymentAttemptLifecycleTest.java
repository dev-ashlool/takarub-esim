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
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

class PaymentAttemptLifecycleTest {

    private static final Instant T0 = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-03-01T10:05:00Z");

    private final IdGenerator idGenerator = new UuidIdGenerator();
    private final AtomicReference<Instant> now = new AtomicReference<>(T0);
    private final ClockProvider clock = new MutableClockProvider(now);

    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        now.set(T0);
        attempt = PaymentAttempt.create(
                idGenerator,
                clock,
                OrderId.of(UUID.randomUUID()),
                new BigDecimal("10.00"),
                "USD");
    }

    @Test
    void assignExternalOrderIdAssignsFirstValueAndUpdatesUpdatedAt() {
        now.set(T1);
        attempt.assignExternalOrderId(clock, "ext-order-1");

        assertThat(attempt.externalOrderId()).isEqualTo("ext-order-1");
        assertThat(attempt.createdAt()).isEqualTo(T0);
        assertThat(attempt.updatedAt()).isEqualTo(T1);
    }

    @Test
    void assignExternalOrderIdTrimsSurroundingWhitespace() {
        attempt.assignExternalOrderId(clock, "  ext-order-1  ");

        assertThat(attempt.externalOrderId()).isEqualTo("ext-order-1");
    }

    @Test
    void assignExternalOrderIdSameNormalizedValueIsIdempotent() {
        attempt.assignExternalOrderId(clock, "ext-order-1");
        Instant afterFirst = attempt.updatedAt();

        now.set(T1);
        attempt.assignExternalOrderId(clock, "  ext-order-1  ");

        assertThat(attempt.externalOrderId()).isEqualTo("ext-order-1");
        assertThat(attempt.updatedAt()).isEqualTo(afterFirst);
    }

    @Test
    void assignExternalOrderIdDifferentValueConflicts() {
        attempt.assignExternalOrderId(clock, "ext-order-1");

        assertThatThrownBy(() -> attempt.assignExternalOrderId(clock, "ext-order-2"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void assignExternalOrderIdNullOrBlankRejected() {
        assertThatThrownBy(() -> attempt.assignExternalOrderId(clock, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> attempt.assignExternalOrderId(clock, "   "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void assignExternalTransactionIdAssignsFirstValueAndUpdatesUpdatedAt() {
        now.set(T1);
        attempt.assignExternalTransactionId(clock, "ext-tx-1");

        assertThat(attempt.externalTransactionId()).isEqualTo("ext-tx-1");
        assertThat(attempt.updatedAt()).isEqualTo(T1);
    }

    @Test
    void assignExternalTransactionIdTrimsAndIsIdempotentForSameValue() {
        attempt.assignExternalTransactionId(clock, "  ext-tx-1  ");
        Instant afterFirst = attempt.updatedAt();

        now.set(T1);
        attempt.assignExternalTransactionId(clock, "ext-tx-1");

        assertThat(attempt.externalTransactionId()).isEqualTo("ext-tx-1");
        assertThat(attempt.updatedAt()).isEqualTo(afterFirst);
    }

    @Test
    void assignExternalTransactionIdDifferentValueConflicts() {
        attempt.assignExternalTransactionId(clock, "ext-tx-1");

        assertThatThrownBy(() -> attempt.assignExternalTransactionId(clock, "ext-tx-2"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void assignExternalTransactionIdNullOrBlankRejected() {
        assertThatThrownBy(() -> attempt.assignExternalTransactionId(clock, null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> attempt.assignExternalTransactionId(clock, " "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void externalIdsMayBeAssignedIndependently() {
        attempt.assignExternalOrderId(clock, "ext-order-1");
        assertThat(attempt.externalOrderId()).isEqualTo("ext-order-1");
        assertThat(attempt.externalTransactionId()).isNull();

        PaymentAttempt other = PaymentAttempt.create(
                idGenerator, clock, OrderId.of(UUID.randomUUID()), new BigDecimal("10.00"), "USD");
        other.assignExternalTransactionId(clock, "ext-tx-1");
        assertThat(other.externalTransactionId()).isEqualTo("ext-tx-1");
        assertThat(other.externalOrderId()).isNull();
    }

    @Test
    void initiatedToConfirmed() {
        now.set(T1);
        attempt.confirm(clock);

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
        assertThat(attempt.createdAt()).isEqualTo(T0);
        assertThat(attempt.updatedAt()).isEqualTo(T1);
    }

    @Test
    void confirmedCannotConfirmAgain() {
        attempt.confirm(clock);

        assertThatThrownBy(() -> attempt.confirm(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void confirmedCannotFail() {
        attempt.confirm(clock);

        assertThatThrownBy(() -> attempt.fail(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void initiatedToFailed() {
        now.set(T1);
        attempt.fail(clock);

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(attempt.updatedAt()).isEqualTo(T1);
    }

    @Test
    void failedCannotFailAgain() {
        attempt.fail(clock);

        assertThatThrownBy(() -> attempt.fail(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void failedCannotConfirm() {
        attempt.fail(clock);

        assertThatThrownBy(() -> attempt.confirm(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void confirmedCannotAssignExternalIds() {
        attempt.confirm(clock);

        assertThatThrownBy(() -> attempt.assignExternalOrderId(clock, "ext-order-1"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> attempt.assignExternalTransactionId(clock, "ext-tx-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void failedCannotAssignExternalIds() {
        attempt.fail(clock);

        assertThatThrownBy(() -> attempt.assignExternalOrderId(clock, "ext-order-1"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> attempt.assignExternalTransactionId(clock, "ext-tx-1"))
                .isInstanceOf(ConflictException.class);
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
