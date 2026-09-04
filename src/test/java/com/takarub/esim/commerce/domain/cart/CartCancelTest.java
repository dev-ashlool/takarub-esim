package com.takarub.esim.commerce.domain.cart;

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

import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

class CartCancelTest {

    private static final Instant T0 = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-03-01T10:05:00Z");

    private final IdGenerator idGenerator = new UuidIdGenerator();
    private final AtomicReference<Instant> now = new AtomicReference<>(T0);
    private final ClockProvider clock = new MutableClockProvider(now);

    private Cart cart;

    @BeforeEach
    void setUp() {
        now.set(T0);
        cart = Cart.create(idGenerator, clock, UserId.of(UUID.randomUUID()));
    }

    @Test
    void openCartCanBeCanceled() {
        now.set(T1);
        cart.cancel(clock);

        assertThat(cart.status()).isEqualTo(CartStatus.CANCELED);
        assertThat(cart.createdAt()).isEqualTo(T0);
        assertThat(cart.updatedAt()).isEqualTo(T1);
    }

    @Test
    void canceledCartCannotBeCanceledAgain() {
        cart.cancel(clock);

        assertThatThrownBy(() -> cart.cancel(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void checkedOutCartCannotBeCanceled() {
        cart.addItem(offer(), 1, clock);
        cart.checkout(clock);

        assertThatThrownBy(() -> cart.cancel(clock)).isInstanceOf(ConflictException.class);
        assertThat(cart.status()).isEqualTo(CartStatus.CHECKED_OUT);
    }

    @Test
    void canceledCartCannotCheckout() {
        cart.addItem(offer(), 1, clock);
        cart.cancel(clock);

        assertThatThrownBy(() -> cart.checkout(clock)).isInstanceOf(ConflictException.class);
    }

    @Test
    void canceledCartCannotAddItem() {
        cart.cancel(clock);

        assertThatThrownBy(() -> cart.addItem(offer(), 1, clock)).isInstanceOf(ConflictException.class);
    }

    private static CartItemOffer offer() {
        return new CartItemOffer(
                "pkg-1",
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                1,
                DataUnit.GB,
                7,
                new BigDecimal("10.00"),
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
