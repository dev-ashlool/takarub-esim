package com.takarub.esim.commerce.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderItemSnapshot;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.takarub.esim.commerce.infrastructure.persistence.entity.OrderJpaEntity;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.identity.shared.time.SystemClockProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Pure mapper round-trip (no Spring / DB). Complements {@code OrderRepositoryAdapterTest} when
 * Flyway+H2 cannot boot the full persistence context.
 */
class OrderPersistenceMapperTest {

    private static final Instant FIXED = Instant.parse("2026-09-04T12:00:00Z");

    private final OrderPersistenceMapper mapper = new OrderPersistenceMapper();
    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final ClockProvider clock = new SystemClockProvider(Clock.fixed(FIXED, ZoneOffset.UTC));

    @Test
    void roundTripsOrderHeaderAndItemsIncludingCheckoutRequestId() {
        CheckoutRequestId checkoutRequestId = CheckoutRequestId.of(UUID.randomUUID().toString());
        Order order = Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                checkoutRequestId,
                List.of(
                        snapshot("pkg-1", "10.00", 1),
                        snapshot("pkg-2", "4.50", 2)));

        OrderJpaEntity entity = mapper.toEntity(order);
        Order reconstituted = mapper.toDomain(entity);

        assertThat(entity.getCheckoutRequestId()).isEqualTo(checkoutRequestId.value());
        assertThat(entity.getItems()).hasSize(2);
        assertThat(reconstituted.id()).isEqualTo(order.id());
        assertThat(reconstituted.cartId()).isEqualTo(order.cartId());
        assertThat(reconstituted.userId()).isEqualTo(order.userId());
        assertThat(reconstituted.checkoutRequestId()).isEqualTo(checkoutRequestId);
        assertThat(reconstituted.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(reconstituted.totalAmount()).isEqualByComparingTo(order.totalAmount());
        assertThat(reconstituted.currency()).isEqualTo("USD");
        assertThat(reconstituted.createdAt()).isEqualTo(FIXED);
        assertThat(reconstituted.updatedAt()).isEqualTo(FIXED);
        assertThat(reconstituted.itemsView()).hasSize(2);
        assertThat(reconstituted.itemsView().get(0).packageId()).isEqualTo("pkg-1");
        assertThat(reconstituted.itemsView().get(1).quantity()).isEqualTo(2);
        assertThat(reconstituted.itemsView().get(1).lineTotal()).isEqualByComparingTo("9.00");
        assertThat(reconstituted.itemsView().get(0).supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(reconstituted.itemsView().get(0).remoteProductId()).isEqualTo("5653");
        assertThat(reconstituted.itemsView().get(0).supplierCostAtCheckout())
                .isEqualByComparingTo("4.7100");
        assertThat(reconstituted.itemsView().get(0).supplierCostCurrency()).isEqualTo("USD");
        assertThat(entity.getItems().get(0).getSupplierKey()).isEqualTo("LIKE_CARD");
        assertThat(entity.getItems().get(0).getRemoteProductId()).isEqualTo("5653");
        assertThat(entity.getItems().get(0).getSupplierCostPrice()).isEqualByComparingTo("4.7100");
        assertThat(entity.getItems().get(0).getSupplierCostCurrency()).isEqualTo("USD");
    }

    @Test
    void mapsLegacyJpaEntityWithAllSupplierFieldsNullIntoDomain() {
        OrderJpaEntity entity = new OrderJpaEntity(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                OrderStatus.PAID,
                new BigDecimal("10.00"),
                "USD",
                FIXED,
                FIXED);
        entity.addItem(new OrderItemJpaEntity(
                "pkg-legacy",
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
                null,
                null,
                null,
                null));

        Order domain = mapper.toDomain(entity);

        assertThat(domain.itemsView()).hasSize(1);
        assertThat(domain.itemsView().get(0).packageId()).isEqualTo("pkg-legacy");
        assertThat(domain.itemsView().get(0).supplierKey()).isNull();
        assertThat(domain.itemsView().get(0).remoteProductId()).isNull();
        assertThat(domain.itemsView().get(0).supplierCostAtCheckout()).isNull();
        assertThat(domain.itemsView().get(0).supplierCostCurrency()).isNull();
    }

    private static OrderItemSnapshot snapshot(String packageId, String unitPrice, int quantity) {
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
                "USD",
                quantity,
                "LIKE_CARD",
                "5653",
                new BigDecimal("4.7100"),
                "USD");
    }
}
