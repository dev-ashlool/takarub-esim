package com.takarub.esim.commerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

class OrderReconstituteTest {

    @Test
    void restoresAllStateWithoutRunningLifecycleTransitions() {
        OrderId id = OrderId.of(UUID.randomUUID());
        CartId cartId = CartId.of(UUID.randomUUID());
        UserId userId = UserId.of(UUID.randomUUID());
        CheckoutRequestId checkoutRequestId = CheckoutRequestId.of(UUID.randomUUID().toString());
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-05T08:00:00Z");
        BigDecimal total = new BigDecimal("29.98");

        OrderItem item = OrderItem.reconstitute(
                "pkg-1",
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                2,
                DataUnit.GB,
                15,
                new BigDecimal("14.99"),
                "USD",
                2,
                "LIKE_CARD",
                "100",
                new BigDecimal("7.0000"),
                "USD");

        Order order = Order.reconstitute(
                id,
                createdAt,
                updatedAt,
                cartId,
                userId,
                checkoutRequestId,
                OrderStatus.PENDING_PAYMENT,
                List.of(item),
                total,
                "USD");

        assertThat(order.id()).isEqualTo(id);
        assertThat(order.cartId()).isEqualTo(cartId);
        assertThat(order.userId()).isEqualTo(userId);
        assertThat(order.checkoutRequestId()).isEqualTo(checkoutRequestId);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.totalAmount()).isEqualByComparingTo(total);
        assertThat(order.currency()).isEqualTo("USD");
        assertThat(order.createdAt()).isEqualTo(createdAt);
        assertThat(order.updatedAt()).isEqualTo(updatedAt);
        assertThat(order.itemsView()).hasSize(1);

        OrderItem restored = order.itemsView().get(0);
        assertThat(restored.packageId()).isEqualTo("pkg-1");
        assertThat(restored.countryIso()).isEqualTo("JO");
        assertThat(restored.countryNameArabic()).isEqualTo("الأردن");
        assertThat(restored.countryNameEnglish()).isEqualTo("Jordan");
        assertThat(restored.locationType()).isEqualTo(LocationType.COUNTRY);
        assertThat(restored.dataAmount()).isEqualTo(2);
        assertThat(restored.dataUnit()).isEqualTo(DataUnit.GB);
        assertThat(restored.durationDays()).isEqualTo(15);
        assertThat(restored.unitPrice()).isEqualByComparingTo("14.99");
        assertThat(restored.currency()).isEqualTo("USD");
        assertThat(restored.quantity()).isEqualTo(2);
        assertThat(restored.lineTotal()).isEqualByComparingTo("29.98");
        assertThat(restored.supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(restored.remoteProductId()).isEqualTo("100");
        assertThat(restored.supplierCostAtCheckout()).isEqualByComparingTo("7.0000");
        assertThat(restored.supplierCostCurrency()).isEqualTo("USD");
    }

    @Test
    void reconstitutesTerminalPaidStatus() {
        CheckoutRequestId checkoutRequestId = CheckoutRequestId.of(UUID.randomUUID().toString());
        Order order = Order.reconstitute(
                OrderId.of(UUID.randomUUID()),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                CartId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                checkoutRequestId,
                OrderStatus.PAID,
                List.of(OrderItem.reconstitute(
                        "pkg-9",
                        "AE",
                        "الإمارات",
                        "UAE",
                        LocationType.COUNTRY,
                        5,
                        DataUnit.GB,
                        30,
                        new BigDecimal("20.00"),
                        "USD",
                        1,
                        "LIKE_CARD",
                        "200",
                        new BigDecimal("8.5000"),
                        "USD")),
                new BigDecimal("20.00"),
                "USD");

        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.checkoutRequestId()).isEqualTo(checkoutRequestId);
        assertThat(order.itemsView()).hasSize(1);
        assertThat(order.itemsView().get(0).supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(order.itemsView().get(0).remoteProductId()).isEqualTo("200");
    }

    @Test
    void reconstitutesLegacyOrderItemWithAllSupplierFieldsNull() {
        OrderItem item = OrderItem.reconstitute(
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
                null);

        assertThat(item.packageId()).isEqualTo("pkg-legacy");
        assertThat(item.supplierKey()).isNull();
        assertThat(item.remoteProductId()).isNull();
        assertThat(item.supplierCostAtCheckout()).isNull();
        assertThat(item.supplierCostCurrency()).isNull();
        assertThat(item.lineTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void reconstitutesRejectsPartialSupplierSnapshot() {
        assertThatThrownBy(() -> OrderItem.reconstitute(
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
                null,
                new BigDecimal("4.7100"),
                "USD"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("partial");
    }
}
