package com.takarub.esim.commerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

class OrderLifecycleTest {

    private static final Instant T0 = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-03-01T10:05:00Z");

    private final IdGenerator idGenerator = new UuidIdGenerator();
    private final AtomicReference<Instant> now = new AtomicReference<>(T0);
    private final ClockProvider clock = new MutableClockProvider(now);

    private Order order;

    @BeforeEach
    void setUp() {
        now.set(T0);
        order = Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                CheckoutRequestId.of(UUID.randomUUID().toString()),
                List.of(snapshot()));
    }

    @Test
    void createdToPendingPayment() {
        now.set(T1);
        order.startPayment(clock);

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.createdAt()).isEqualTo(T0);
        assertThat(order.updatedAt()).isEqualTo(T1);
    }

    @Test
    void paymentFailedToPendingPaymentOnRetry() {
        order.startPayment(clock);
        order.markPaymentFailed(clock);
        assertThat(order.status()).isEqualTo(OrderStatus.PAYMENT_FAILED);

        now.set(T1);
        order.startPayment(clock);

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.createdAt()).isEqualTo(T0);
        assertThat(order.updatedAt()).isEqualTo(T1);
    }

    @Test
    void pendingPaymentToPaid() {
        order.startPayment(clock);
        now.set(T1);
        order.markPaid(clock);

        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.createdAt()).isEqualTo(T0);
        assertThat(order.updatedAt()).isEqualTo(T1);
    }

    @Test
    void pendingPaymentToPaymentFailed() {
        order.startPayment(clock);
        now.set(T1);
        order.markPaymentFailed(clock);

        assertThat(order.status()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.createdAt()).isEqualTo(T0);
        assertThat(order.updatedAt()).isEqualTo(T1);
    }

    @Test
    void createdCannotMarkPaid() {
        assertThatThrownBy(() -> order.markPaid(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void createdCannotMarkPaymentFailed() {
        assertThatThrownBy(() -> order.markPaymentFailed(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void paidCannotStartPayment() {
        order.startPayment(clock);
        order.markPaid(clock);

        assertThatThrownBy(() -> order.startPayment(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void paidCannotMarkPaymentFailed() {
        order.startPayment(clock);
        order.markPaid(clock);

        assertThatThrownBy(() -> order.markPaymentFailed(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void paidCannotMarkPaidAgain() {
        order.startPayment(clock);
        order.markPaid(clock);

        assertThatThrownBy(() -> order.markPaid(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void paymentFailedCannotMarkPaid() {
        order.startPayment(clock);
        order.markPaymentFailed(clock);

        assertThatThrownBy(() -> order.markPaid(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void pendingPaymentCannotStartPaymentAgain() {
        order.startPayment(clock);

        assertThatThrownBy(() -> order.startPayment(clock)).isInstanceOf(ConflictException.class);
    }

    private static OrderItemSnapshot snapshot() {
        return new OrderItemSnapshot(
                "pkg-1",
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                1,
                DataUnit.GB,
                7,
                new BigDecimal("10.00"),
                "USD",
                1,
                "LIKE_CARD",
                "5653",
                new BigDecimal("4.7100"),
                "USD");
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
