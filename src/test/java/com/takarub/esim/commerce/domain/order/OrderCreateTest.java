package com.takarub.esim.commerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.BusinessException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.identity.shared.time.SystemClockProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

class OrderCreateTest {

    private static final Instant FIXED = Instant.parse("2026-03-01T10:00:00Z");

    private final IdGenerator idGenerator = new UuidIdGenerator();
    private final ClockProvider clock = new SystemClockProvider(Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void createStartsCreatedWithGeneratedIdAndDerivedTotals() {
        CartId cartId = CartId.of(UUID.randomUUID());
        UserId userId = UserId.of(UUID.randomUUID());
        OrderItemSnapshot line = snapshot("pkg-1", "USD", "9.99", 2);

        Order order = Order.create(idGenerator, clock, cartId, userId, List.of(line));

        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.id()).isNotNull();
        assertThat(order.id().value()).isNotNull();
        assertThat(order.cartId()).isEqualTo(cartId);
        assertThat(order.userId()).isEqualTo(userId);
        assertThat(order.createdAt()).isEqualTo(FIXED);
        assertThat(order.updatedAt()).isEqualTo(FIXED);
        assertThat(order.currency()).isEqualTo("USD");
        assertThat(order.totalAmount()).isEqualByComparingTo("19.98");
        assertThat(order.itemsView()).hasSize(1);
        assertThat(order.itemsView().get(0).packageId()).isEqualTo("pkg-1");
        assertThat(order.itemsView().get(0).lineTotal()).isEqualByComparingTo("19.98");
    }

    @Test
    void createDerivesMultiItemTotalAndCurrency() {
        Order order = Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                List.of(
                        snapshot("pkg-1", "USD", "10.00", 1),
                        snapshot("pkg-2", "USD", "5.50", 3)));

        assertThat(order.totalAmount()).isEqualByComparingTo("26.50");
        assertThat(order.currency()).isEqualTo("USD");
        assertThat(order.itemsView()).hasSize(2);
    }

    @Test
    void itemsViewIsUnmodifiable() {
        Order order = Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                List.of(snapshot("pkg-1", "USD", "10.00", 1)));

        assertThatThrownBy(() -> order.itemsView().add(order.itemsView().get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyItemsRejected() {
        assertThatThrownBy(() -> Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    void nullItemsRejected() {
        assertThatThrownBy(() -> Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void nullCartIdRejected() {
        assertThatThrownBy(() -> Order.create(
                idGenerator,
                clock,
                null,
                UserId.of(UUID.randomUUID()),
                List.of(snapshot("pkg-1", "USD", "10.00", 1))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cart id");
    }

    @Test
    void nullUserIdRejected() {
        assertThatThrownBy(() -> Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                null,
                List.of(snapshot("pkg-1", "USD", "10.00", 1))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User id");
    }

    @Test
    void nonPositiveQuantityRejected() {
        assertThatThrownBy(() -> Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                List.of(snapshot("pkg-1", "USD", "10.00", 0))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void nonPositivePriceRejected() {
        assertThatThrownBy(() -> Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                List.of(snapshot("pkg-1", "USD", "0", 1))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("unitPrice");
    }

    @Test
    void mixedCurrenciesRejected() {
        assertThatThrownBy(() -> Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                List.of(
                        snapshot("pkg-1", "USD", "10.00", 1),
                        snapshot("pkg-2", "JOD", "5.00", 1))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("same currency");
    }

    @Test
    void blankPackageIdRejected() {
        OrderItemSnapshot invalid = new OrderItemSnapshot(
                " ",
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                1,
                DataUnit.GB,
                7,
                new BigDecimal("10.00"),
                "USD",
                1);

        assertThatThrownBy(() -> Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                List.of(invalid)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("packageId");
    }

    @Test
    void requiredCommercialFieldsFollowCartRules() {
        List<OrderItemSnapshot> missingLocation = new ArrayList<>();
        missingLocation.add(new OrderItemSnapshot(
                "pkg-1",
                "JO",
                "الأردن",
                "Jordan",
                null,
                1,
                DataUnit.GB,
                7,
                new BigDecimal("10.00"),
                "USD",
                1));

        assertThatThrownBy(() -> Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                missingLocation))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("locationType");
    }

    private static OrderItemSnapshot snapshot(String packageId, String currency, String unitPrice, int quantity) {
        return new OrderItemSnapshot(
                packageId,
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                1,
                DataUnit.GB,
                7,
                new BigDecimal(unitPrice),
                currency,
                quantity);
    }
}
