package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

import com.takarub.esim.commerce.application.exception.EsimNotReadyApplicationException;
import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.CustomerEsimActivation;
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
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsim;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimId;
import com.takarub.esim.commerce.domain.provisioning.ProvisionedEsimRepository;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ForbiddenException;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@ExtendWith(MockitoExtension.class)
class GetCustomerEsimUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-06T10:00:00Z");

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private FulfillmentWorkRepository fulfillmentWorkRepository;
    @Mock
    private ProvisionedEsimRepository provisionedEsimRepository;
    @Mock
    private ClockProvider clock;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private GetCustomerEsimUseCase useCase;
    private UserId userId;

    @BeforeEach
    void setUp() {
        userId = UserId.of(UUID.randomUUID());
        useCase = new GetCustomerEsimUseCase(
                orderRepository, fulfillmentWorkRepository, provisionedEsimRepository);
        lenient().when(clock.now()).thenReturn(NOW);
    }

    @Test
    void ownFulfilledOrderWithEsimReturnsActivation() {
        Order order = paidOrder(userId);
        FulfillmentWork work = fulfillment(order.id(), FulfillmentStatus.FULFILLED);
        ProvisionedEsim esim = provisioned(order.id(), work.id());
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(fulfillmentWorkRepository.findByOrderId(order.id())).thenReturn(Optional.of(work));
        when(provisionedEsimRepository.findByOrderId(order.id())).thenReturn(Optional.of(esim));

        CustomerEsimActivation result = useCase.execute(userId, order.id());

        assertThat(result.orderId()).isEqualTo(order.id());
        assertThat(result.iccid()).isEqualTo("8901");
        assertThat(result.qrString()).isEqualTo("LPA:1$fake.smdp$ACT-TEST");
        assertThat(result.smdpAddress()).isNull();
        assertThat(result.activationCode()).isNull();
        assertThat(result.pin()).isEqualTo("1234");
        assertThat(result.puk()).isEqualTo("5678");
    }

    @Test
    void missingOrderThrowsNotFound() {
        OrderId orderId = OrderId.of(UUID.randomUUID());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(userId, orderId))
                .isInstanceOf(OrderNotFoundApplicationException.class);

        verify(fulfillmentWorkRepository, never()).findByOrderId(any());
        verify(provisionedEsimRepository, never()).findByOrderId(any());
    }

    @Test
    void foreignOwnerThrowsForbiddenWithoutFulfillmentOrEsimLookup() {
        Order order = paidOrder(UserId.of(UUID.randomUUID()));
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.execute(userId, order.id()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not own");

        verify(fulfillmentWorkRepository, never()).findByOrderId(any());
        verify(provisionedEsimRepository, never()).findByOrderId(any());
    }

    @ParameterizedTest
    @EnumSource(
            value = FulfillmentStatus.class,
            names = {"PENDING", "PROCESSING", "UNKNOWN", "BLOCKED"})
    void nonFulfilledStatusThrowsNotReady(FulfillmentStatus status) {
        Order order = paidOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(fulfillmentWorkRepository.findByOrderId(order.id()))
                .thenReturn(Optional.of(fulfillment(order.id(), status)));

        assertThatThrownBy(() -> useCase.execute(userId, order.id()))
                .isInstanceOf(EsimNotReadyApplicationException.class)
                .hasMessageContaining("not ready");

        verify(provisionedEsimRepository, never()).findByOrderId(any());
    }

    @Test
    void missingFulfillmentThrowsNotReady() {
        Order order = paidOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(fulfillmentWorkRepository.findByOrderId(order.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(userId, order.id()))
                .isInstanceOf(EsimNotReadyApplicationException.class);

        verify(provisionedEsimRepository, never()).findByOrderId(any());
    }

    @Test
    void fulfilledWithoutProvisionedEsimThrowsNotReady() {
        Order order = paidOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(fulfillmentWorkRepository.findByOrderId(order.id()))
                .thenReturn(Optional.of(fulfillment(order.id(), FulfillmentStatus.FULFILLED)));
        when(provisionedEsimRepository.findByOrderId(order.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(userId, order.id()))
                .isInstanceOf(EsimNotReadyApplicationException.class)
                .hasMessageContaining("not ready");
    }

    private Order paidOrder(UserId owner) {
        Order order = Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                owner,
                CheckoutRequestId.of(UUID.randomUUID().toString()),
                List.of(line()));
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
                FulfillmentId.generate(idGenerator),
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

    private ProvisionedEsim provisioned(OrderId orderId, FulfillmentId workId) {
        return ProvisionedEsim.reconstitute(
                ProvisionedEsimId.generate(idGenerator),
                orderId,
                workId,
                "LIKE_CARD",
                "5653",
                "sup-order-1",
                "8901",
                null,
                null,
                "1234",
                "5678",
                "LPA:1$fake.smdp$ACT-TEST",
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
