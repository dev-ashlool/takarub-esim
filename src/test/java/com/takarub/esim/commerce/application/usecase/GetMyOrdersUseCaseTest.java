package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.CustomerOrderDetails;
import com.takarub.esim.commerce.application.result.CustomerOrderSummary;
import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderItemSnapshot;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ForbiddenException;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@ExtendWith(MockitoExtension.class)
class GetMyOrdersUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-06T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private FulfillmentWorkRepository fulfillmentWorkRepository;
    @Mock
    private ClockProvider clock;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private GetMyOrdersUseCase useCase;
    private UserId userId;

    @BeforeEach
    void setUp() {
        userId = UserId.of(UUID.randomUUID());
        useCase = new GetMyOrdersUseCase(orderRepository, fulfillmentWorkRepository);
        lenient().when(clock.now()).thenReturn(NOW);
    }

    @Test
    void returnsOwnOrdersNewestFirstWithNullFulfillmentWhenAbsent() {
        Order newer = paidOrder(userId);
        Order older = createdOrder(userId);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(newer, older));
        when(fulfillmentWorkRepository.findByOrderId(newer.id())).thenReturn(Optional.empty());
        when(fulfillmentWorkRepository.findByOrderId(older.id())).thenReturn(Optional.empty());

        List<CustomerOrderSummary> result = useCase.execute(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).orderId()).isEqualTo(newer.id());
        assertThat(result.get(1).orderId()).isEqualTo(older.id());
        assertThat(result.get(0).fulfillmentStatus()).isNull();
        assertThat(result.get(0).orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.get(0).items()).hasSize(1);
        assertThat(result.get(0).items().getFirst().packageId()).isEqualTo("pkg-1");
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void emptyListWhenUserHasNoOrders() {
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        assertThat(useCase.execute(userId)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(FulfillmentStatus.class)
    void mapsFulfillmentStatuses(FulfillmentStatus status) {
        Order order = paidOrder(userId);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(order));
        when(fulfillmentWorkRepository.findByOrderId(order.id()))
                .thenReturn(Optional.of(fulfillment(order.id(), status)));

        List<CustomerOrderSummary> result = useCase.execute(userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().fulfillmentStatus()).isEqualTo(status);
    }

    private Order createdOrder(UserId owner) {
        return Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                owner,
                CheckoutRequestId.of(UUID.randomUUID().toString()),
                List.of(line()));
    }

    private Order paidOrder(UserId owner) {
        Order order = createdOrder(owner);
        order.startPayment(clock);
        order.markPaid(clock);
        return order;
    }

    private FulfillmentWork fulfillment(OrderId orderId, FulfillmentStatus status) {
        Instant claimedAt = status == FulfillmentStatus.PENDING || status == FulfillmentStatus.BLOCKED
                ? null
                : NOW;
        String errorCode = (status == FulfillmentStatus.UNKNOWN || status == FulfillmentStatus.BLOCKED)
                ? "CODE"
                : null;
        String errorMessage = errorCode == null ? null : "msg";
        return FulfillmentWork.reconstitute(
                com.takarub.esim.commerce.domain.fulfillment.FulfillmentId.generate(idGenerator),
                orderId,
                "LIKE_CARD",
                "5653",
                status,
                claimedAt,
                errorCode,
                errorMessage,
                NOW,
                NOW);
    }

    private static OrderItemSnapshot line() {
        return new OrderItemSnapshot(
                "pkg-1",
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                1,
                DataUnit.GB,
                7,
                new BigDecimal("9.99"),
                "USD",
                1,
                "LIKE_CARD",
                "5653",
                new BigDecimal("4.7100"),
                "USD");
    }
}
