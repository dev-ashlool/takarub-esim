package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.CustomerOrderDetails;
import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentId;
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
class GetOrderDetailsUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-06T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private FulfillmentWorkRepository fulfillmentWorkRepository;
    @Mock
    private ClockProvider clock;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private GetOrderDetailsUseCase useCase;
    private UserId userId;

    @BeforeEach
    void setUp() {
        userId = UserId.of(UUID.randomUUID());
        useCase = new GetOrderDetailsUseCase(orderRepository, fulfillmentWorkRepository);
        lenient().when(clock.now()).thenReturn(NOW);
    }

    @Test
    void ownOrderSuccessWithFulfillment() {
        Order order = paidOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(fulfillmentWorkRepository.findByOrderId(order.id()))
                .thenReturn(Optional.of(pendingFulfillment(order.id())));

        CustomerOrderDetails details = useCase.execute(userId, order.id());

        assertThat(details.orderId()).isEqualTo(order.id());
        assertThat(details.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(details.fulfillmentStatus()).isEqualTo(FulfillmentStatus.PENDING);
        assertThat(details.updatedAt()).isEqualTo(order.updatedAt());
        assertThat(details.items()).hasSize(1);
        assertThat(details.items().getFirst().packageId()).isEqualTo("pkg-1");
    }

    @Test
    void ownOrderWithoutFulfillmentReturnsNullStatus() {
        Order order = createdOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(fulfillmentWorkRepository.findByOrderId(order.id())).thenReturn(Optional.empty());

        CustomerOrderDetails details = useCase.execute(userId, order.id());

        assertThat(details.fulfillmentStatus()).isNull();
        assertThat(details.orderStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void missingOrderThrowsNotFound() {
        OrderId orderId = OrderId.of(UUID.randomUUID());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(userId, orderId))
                .isInstanceOf(OrderNotFoundApplicationException.class);
    }

    @Test
    void foreignOwnerThrowsForbidden() {
        Order order = createdOrder(UserId.of(UUID.randomUUID()));
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.execute(userId, order.id()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not own");
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

    private FulfillmentWork pendingFulfillment(OrderId orderId) {
        return FulfillmentWork.reconstitute(
                FulfillmentId.generate(idGenerator),
                orderId,
                "LIKE_CARD",
                "5653",
                FulfillmentStatus.PENDING,
                null,
                null,
                null,
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
